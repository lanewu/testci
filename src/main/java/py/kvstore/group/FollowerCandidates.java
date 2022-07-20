package py.kvstore.group;

import com.google.gson.Gson;

import java.util.*;

public class FollowerCandidates {
    private Map<Long, Long> followers;

    public FollowerCandidates(Map<Long, Long> followers) {
        this.followers = followers;
    }

    public Map<Long, Long> getCandidates() {
        return new HashMap<>(followers);
    }

    public void addCandidate(Long candidate, long leaseId) {
        followers.put(candidate, leaseId);
    }

    public boolean isCandidate(Long id) {
        Objects.requireNonNull(id);
        return followers.containsKey(id);
    }

    public void clear() {
        followers.clear();
    }

    public static FollowerCandidates fromJson(String groupJson) {
        Gson gson = new Gson();
        return  gson.fromJson(groupJson, FollowerCandidates.class);
    }

    public int size() {
        return followers.size();
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        return "FollowerCandidates{" +
                ", followers=" + followers +
                '}';
    }
}
