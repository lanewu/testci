package py.kvstore.group;

import com.google.common.collect.Maps;
import io.netty.util.*;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.common.NamedThreadFactory;
import py.kvstore.KvStore;
import py.kvstore.KvStoreException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GroupOrganizer {

    private static final Logger logger = LoggerFactory.getLogger(GroupOrganizer.class);

    private static final int MIN_LEASE_SPAN_SECONDS = 10;
    private static final int MAX_LEASE_SPAN_SECONDS = 60;

    private final int leaseSpanSeconds;
    private final int processPeriodMS;
    private final int updateMembershipDelayMS;
    private long startTime = 0l;

    private KvStore kvClient;
    private Long myself;
    private String groupKey;
    private String followerCandidateKey;
    private long myLease;
    private Map<Long, Long> peerLeases;
    private Group group;
    private Group prevGroup;
    private ScheduledExecutorService stateProcessEngine;
    private HashedWheelTimer hashedWheelTimer;
    private boolean paused;
    private GroupListener listener;

    public interface GroupListener {
        void groupUpdated(Group group);
        void leaseExtended();
    }

    private static int properLeaseSpan(int wantedLeaseSpan) {

      if (wantedLeaseSpan < MIN_LEASE_SPAN_SECONDS) {
          logger.error("the given lease span is too small {}, using the min value instead {}",
              wantedLeaseSpan, MIN_LEASE_SPAN_SECONDS);
          return MIN_LEASE_SPAN_SECONDS;
      }

      if (wantedLeaseSpan > MAX_LEASE_SPAN_SECONDS) {
          logger.error("the given lease span is too big {}, using the min value instead {}",
              wantedLeaseSpan, MAX_LEASE_SPAN_SECONDS);
          return MAX_LEASE_SPAN_SECONDS;
      }

      logger.warn("lease span {}", wantedLeaseSpan);
      return wantedLeaseSpan;

    }

    public GroupOrganizer(Long myself, String groupKey, KvStore kvClient, GroupListener listener, int leaseSpanSeconds) {
        this.listener = listener;
        this.kvClient = kvClient;
        this.myself = myself;
        this.groupKey = groupKey;
        int leaseSpanSecondsTmp = properLeaseSpan(leaseSpanSeconds);
        this.processPeriodMS = Math
            .max(1000, (int) TimeUnit.SECONDS.toMillis(leaseSpanSecondsTmp) / 10);
        this.leaseSpanSeconds = leaseSpanSecondsTmp - (int) TimeUnit.MILLISECONDS
            .toSeconds(processPeriodMS * 2);//make sure etcd lease < lease space in memory
        logger.warn("the lease time is {}", this.leaseSpanSeconds);
        this.updateMembershipDelayMS = processPeriodMS * 5;
        this.peerLeases = new HashMap<>();
        this.followerCandidateKey = groupKey + ".follower.candidate";
        this.stateProcessEngine = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "etcd-group-process-" + myself));
        this.hashedWheelTimer = new HashedWheelTimer(new NamedThreadFactory("update-group" + myself));
    }

    public void start() {
        stateProcessEngine.scheduleAtFixedRate(() -> {
            try {
                if (!paused) {
                    process();
                }
            } catch (Throwable t) {
                logger.error("", t);
            }
        }, 0, processPeriodMS, TimeUnit.MILLISECONDS);

        hashedWheelTimer.start();
    }

    public void stop() {
        stateProcessEngine.shutdown();
        hashedWheelTimer.stop();
    }

    public void pause() {
        paused = true;
    }

    public void revive() {
        paused = false;
    }

    public Group getGroup() {
        return group;
    }

    private void extendLease() throws KvStoreException {
        startTime = System.currentTimeMillis();
        kvClient.keepAliveOnce(myLease);
        listener.leaseExtended();
        logger.info("extend lease for {}", myLease);
    }

    private void startLease() throws Exception {
        myLease = kvClient.grandLease(leaseSpanSeconds);
        logger.info("start a new lease {}", myLease, new Exception());
    }

    private boolean leaseExpired(long leaseId) throws KvStoreException {
        long ttl = kvClient.timeToLive(leaseId);
        logger.info("lease {} ttl {}", leaseId, ttl);
        return ttl < 0;
    }

    private void requestNewGroup() {
        try {
            String groupJson = kvClient.getStr(groupKey);
            prevGroup = group;
            group = groupJson == null ? null : Group.fromJson(groupJson);
            if (!Objects.equals(group, prevGroup)) {
                logger.warn("got a new group {} prev {} , cost time {}", group, prevGroup,
                    System.currentTimeMillis() - startTime);
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private FollowerCandidates requestNewFollowerCandidates() {
        try {
            String fcJson = kvClient.getStr(followerCandidateKey);
            return fcJson == null ? null : FollowerCandidates.fromJson(fcJson);
        } catch (Exception e) {
        }
        return null;
    }

    private boolean groupExist() {
        return group != null;
    }

    private void process() throws Exception {
        Group startGroup = group;

        requestNewGroup();
        if (groupExist()) {
            if (group.isMaster(myself)) {
                if (leaseExpired(myLease)) {
                    elect();
                } else {
                    extendLease();
                    allowFollowerCandidateJoinGroup();
                    removeExpiredFollower();
                }
            } else if(group.isFollower(myself)) {
                extendLease();
            } else {
                becomeFollowerCandidate();
            }
        } else {
            elect();
        }

        if (!Objects.equals(group, startGroup)) {
            logger.warn("group updated from {} to {}", startGroup, group);
            updateGroup();
        }

        logger.info("current {}", group);
    }


    private void updateGroup() {
        hashedWheelTimer.newTimeout(new TimerTask() {
            @Override
            public void run(Timeout timeout) throws Exception {
                listener.groupUpdated(group);
            }
        }, updateMembershipDelayMS, TimeUnit.MILLISECONDS);
    }
    private void elect() throws Exception {
        logger.info("start elect for {} i am {}", groupKey, myself);
        startLease();
        if (groupExist()) {
            logger.info("just wait the the old group expired {}", group);
            return;
        } else {
            Group.Version version = new Group.Version(prevGroup == null ? 1 : prevGroup.getVersion().getMajor() + 1, 0);
            Group masterRaceGroup = new Group(version, myself, new HashSet<>());
            String exist = kvClient.putIfAbsent(groupKey, masterRaceGroup.toJson(), myLease);
            if (exist == null) {
                //i am become master
                group = masterRaceGroup;
                allowFollowerCandidateJoinGroup();
                logger.warn("new Group @ {}", group);
            } else {
                becomeFollowerCandidate();
            }
        }
    }

    private void allowFollowerCandidateJoinGroup() throws Exception {
        FollowerCandidates followerCandidates = requestNewFollowerCandidates();
        if (followerCandidates == null || followerCandidates.size() == 0) {
            return;
        }
        logger.info("begin new follower join @ {} {}", group, followerCandidates);
        Set<Long> newFollowers = group.getFollowers();
        int incMinor = 0;
        for (Map.Entry<Long, Long> entry : followerCandidates.getCandidates().entrySet()) {
            if (newFollowers.add(entry.getKey())) {
                incMinor++;
                peerLeases.put(entry.getKey(), entry.getValue());
            }
        }
        if (incMinor > 0) {
            Group newGroup = new Group(group.getVersion().incMinor(incMinor), myself, newFollowers);
            kvClient.put(groupKey.getBytes(), newGroup.toJson().getBytes(), myLease);
            group = newGroup;
            logger.warn("after new follower join @ {}", group);
        }
        String old = followerCandidates.toJson();
        followerCandidates.clear();
        boolean success = kvClient.replace(followerCandidateKey, old, followerCandidates.toJson(), myLease);
        if (!success) {
            allowFollowerCandidateJoinGroup();
        }
    }

    private void removeExpiredFollower() throws Exception {
        Set<Long> newFollowers = group.getFollowers();
        Iterator<Long> iter = newFollowers.iterator();
        int incMinor = 0;
        while (iter.hasNext()) {
            Long follower = iter.next();
            if (leaseExpired(peerLeases.get(follower))) {
                logger.warn("lease expired of follower {}", follower);
                peerLeases.remove(follower);
                iter.remove();
                incMinor++;
            }
        }
        if (incMinor > 0) {
            Group newGroup = new Group(group.getVersion().incMinor(incMinor), myself, newFollowers);
            kvClient.put(groupKey.getBytes(), newGroup.toJson().getBytes(), myLease);
            group = newGroup;
            logger.warn("after new follower remove @ {}", group);
        }
    }

    private void becomeFollowerCandidate() throws Exception {
        FollowerCandidates followerCandidates = requestNewFollowerCandidates();
        String exist = null;
        if (followerCandidates == null) {
            if (leaseExpired(myLease)) {
                startLease();
            }
            followerCandidates = new FollowerCandidates(Maps.newHashMap());
            followerCandidates.addCandidate(myself, myLease);
            exist = kvClient.putIfAbsent(followerCandidateKey, followerCandidates.toJson(), myLease);
            if (exist != null) {
                logger.warn("new follower candidate before me @ {}", followerCandidates);
                followerCandidates = FollowerCandidates.fromJson(exist);
            }
        } else {
            exist = followerCandidates.toJson();
        }

        if (!followerCandidates.isCandidate(myself)) {
            startLease();
            followerCandidates.addCandidate(myself, myLease);
            boolean success = kvClient.replace(followerCandidateKey, exist, followerCandidates.toJson(), myLease);
            if (!success) {
                //recursive
                becomeFollowerCandidate();
            }
        }
        logger.warn("new follower Candidate @ {} I am {}", followerCandidates, myself);
    }
}