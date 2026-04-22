package com.vanillage.raytraceantixray.data;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Objects;

public record VectorialLocation(Vector position, Vector direction) {

    public VectorialLocation(Vector position, Vector direction) {
        this.position = Objects.requireNonNull(position);
        this.direction = Objects.requireNonNull(direction);
    }

    public VectorialLocation(VectorialLocation location) {
        this(location.position().clone(), location.direction().clone());
    }

    public VectorialLocation(Location location) {
        this(location.toVector(), location.getDirection());
    }

}
