package com.benji.oasiso.client.soul;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

public final class SoulMesh {
    public record Vertex(float x, float y, float z, float u, float v) {
    }

    public final List<Vertex> vertices = new ArrayList<>();
    public final List<Vector3f> headPoints = new ArrayList<>();

    public VertexConsumer capture(VertexConsumer delegate, boolean head) {
        return new Capture(delegate, head);
    }

    public Vector3f center() {
        var result = new Vector3f();
        var points = headPoints;
        for (var p : points) result.add(p);
        return points.isEmpty() ? result : result.div(points.size());
    }

    private final class Capture implements VertexConsumer {
        private final VertexConsumer out;
        private final boolean head;
        private float x, y, z, u, v;

        Capture(VertexConsumer out, boolean head) {
            this.out = out;
            this.head = head;
        }

        public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            out.vertex(x, y, z);
            return this;
        }

        public VertexConsumer color(int r, int g, int b, int a) {
            out.color(r, g, b, a);
            return this;
        }

        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            out.uv(u, v);
            return this;
        }

        public VertexConsumer overlayCoords(int u, int v) {
            out.overlayCoords(u, v);
            return this;
        }

        public VertexConsumer uv2(int u, int v) {
            out.uv2(u, v);
            return this;
        }

        public VertexConsumer normal(float x, float y, float z) {
            out.normal(x, y, z);
            return this;
        }

        public void endVertex() {
            vertices.add(new Vertex(x, y, z, u, v));
            if (head) headPoints.add(new Vector3f(x, y, z));
            out.endVertex();
        }

        public void defaultColor(int r, int g, int b, int a) {
            out.defaultColor(r, g, b, a);
        }

        public void unsetDefaultColor() {
            out.unsetDefaultColor();
        }
    }
}
