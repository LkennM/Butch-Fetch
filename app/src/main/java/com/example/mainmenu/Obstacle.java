package com.example.mainmenu;


public class Obstacle  {
    public enum Type {
        MANHOLE, CONE, TRASHCAN, FOG, TREATS, TRAFFIC_CONE
    }

    private Type type;
    private int lane;
    private float speed;

    public Obstacle(Type type, int lane, float speed) {
        this.type = type;
        this.lane = lane;
        this.speed = speed;
    }

    public Type getType() {
        return type;
    }

    public int getLane() {
        return lane;
    }

    public float getSpeed() {
        return speed;
    }
}