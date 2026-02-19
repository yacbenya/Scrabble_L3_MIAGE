package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Coup {
    private final List<Pose> poses;
    private final Direction direction;

    public Coup(List<Pose> poses, Direction direction) {
        this.poses = new ArrayList<>(poses);
        this.direction = direction;
    }

    public List<Pose> getPoses() {
        return Collections.unmodifiableList(poses);
    }

    public Direction getDirection() {
        return direction;
    }
}
