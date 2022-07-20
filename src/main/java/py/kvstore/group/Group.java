package py.kvstore.group;

import com.google.gson.Gson;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Group {
    private Version version;
    private Long master;
    private Set<Long> followers;

    public static class Version {
        private int major;
        private int minor;

        public Version(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }

        public Version incMajor() {
            return new Version(major + 1, minor);
        }

        public Version incMinor(int inc) {
            return new Version(major, minor + inc);
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        @Override
        public String toString() {
            return "Version{" +
                    "major=" + major +
                    ", minor=" + minor +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Version)) {
                return false;
            }
            Version version = (Version) o;
            return major == version.major &&
                minor == version.minor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor);
        }
    }

    public Group(Long master) {
        this(new Version(1, 0), master, new HashSet<>());
    }

    public Group(Version version, Long master, Set<Long> followers) {
        this.version = version;
        this.master = master;
        this.followers = followers;
    }

    public Version getVersion() {
        return version;
    }

    public Long getMaster() {
        return master;
    }

    public Set<Long> getFollowers() {
        return new HashSet<>(followers);
    }

    public static Group fromJson(String groupJson) {
        Gson gson = new Gson();
        return  gson.fromJson(groupJson, Group.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public boolean isMaster(Long id) {
        Objects.requireNonNull(id);
        return id.equals(master);
    }

    public boolean isFollower(Long id) {
        Objects.requireNonNull(id);
        return followers.contains(id);
    }

    @Override
    public String toString() {
        return "Group{" +
                ", version=" + version +
                ", master='" + master + '\'' +
                ", followers=" + followers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group)) {
            return false;
        }
        Group group = (Group) o;
        return Objects.equals(version, group.version) &&
            Objects.equals(master, group.master) &&
            Objects.equals(followers, group.followers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, master, followers);
    }
}
