package com.benji.oasiso.dialogue.data;

import java.util.ArrayList;
import java.util.List;

public class DialogueDefinition {

    public int format = 2;

    // Voice
    public String voice;
    public String voice_source = "master";

    public float voice_pitch = 1.0F;
    public float voice_volume = 0.55F;
    public int voice_every = 1;

    // Typewriter
    public int char_ticks = 2;
    public int hold_ticks = 20;
    public int fade_ticks = 14;

    // Text
    public String text_color = "white";
    public List<String> text_gradient;
    public String text_effect = "wave";

    public String text_style;

    // Visuals
    public String frame;
    public String background;

    public float background_alpha = 0.62F;
    public float background_bob = 1.6F;
    public float background_speed = 1.1F;

    // Sprite
    public String sprite_position = "center";
    public String sprite_transition = "bounce";

    public int sprite_move_ticks = 10;
    public int sprite_transition_ticks = 8;

    // Gameplay
    public boolean freeze_source = true;
    public boolean source_invulnerable = true;
    public boolean cancel_if_source_missing = true;

    public boolean exclusive_source = true;

    public String once = "never";

    public Layout layout = new Layout();

    public List<Trigger> triggers = new ArrayList<>();
    public List<Line> lines = new ArrayList<>();


    public static class Layout {

        public int canvas_width = 192;
        public int canvas_height = 108;

        public int frame_x = 0;
        public int frame_y = 63;
        public int frame_width = 192;
        public int frame_height = 45;

        public int text_x = 23;
        public int text_y = 75;
        public int text_width = 146;

        public float text_scale = 0.72F;
        public int line_height = 10;

        public int sprite_width = 126;
        public int sprite_height = 78;
        public int sprite_y = 0;

        public float sprite_left_x = 0.0F;
        public float sprite_center_x = 33.0F;
        public float sprite_right_x = 66.0F;
    }


    public static class Line {

        // Translation key
        public String text;

        public String literal;

        public String sprite;

        // Line overrides
        public Integer char_ticks;
        public Integer hold_ticks;

        public String voice;
        public String voice_source;
        public Float voice_pitch;
        public Float voice_volume;
        public Integer voice_every;

        public String text_color;
        public List<String> text_gradient;

        public String text_effect;

        // legacy
        public String text_style;

        public String frame;
        public String background;

        public String sprite_position;
        public Float sprite_x;

        public String sprite_transition;

        public Integer sprite_move_ticks;
        public Integer sprite_transition_ticks;

        public Integer sprite_width;
        public Integer sprite_height;
    }


    public static class Trigger {
        public String type = "manual";
        public String target;

        public double radius = 5.0D;
        public double look_angle = 12.0D;

        public int check_interval = 5;
        public int cooldown_ticks = 40;

        public boolean consume = false;

        // Override dialogue-level once.
        public String once;

        // External event ID.
        public String event;

        // Dimension filter.
        public String dimension;

        public Double x;
        public Double y;
        public Double z;

        public Double min_x;
        public Double min_y;
        public Double min_z;

        public Double max_x;
        public Double max_y;
        public Double max_z;

        public ZoneAnchor anchor;
        public String shape = "cylinder";

        public double height = 2.0D;
        public double size_x = 6.0D;
        public double size_y = 2.0D;
        public double size_z = 6.0D;
        public ZoneVisual visual = new ZoneVisual();
    }


    public static class ZoneAnchor {

        public String type = "absolute";
        public String target;
        public String entity_tag;
        public String pick = "nearest";


        public Double x;
        public Double y;
        public Double z;


        public double offset_x = 0.0D;
        public double offset_y = 0.0D;
        public double offset_z = 0.0D;

        public double search_height = 8.0D;
    }


    public static class ZoneVisual {

        public boolean enabled = true;
        public String style = "auto";
        public String texture;

        public String color = "cyan";
        public float alpha = 0.55F;
        public double y_offset = 0.03D;
        public double size = 0.0D;

        public double visual_height = 0.0D;
        public boolean pulse = true;
        public double preview_distance = 16.0D;
    }
}