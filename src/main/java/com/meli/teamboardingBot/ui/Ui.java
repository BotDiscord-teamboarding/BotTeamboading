package com.meli.teamboardingBot.ui;

import net.dv8tion.jda.api.EmbedBuilder;

public class Ui {
    public static final int INFO = 0x0099FF;
    public static final int SUCCESS = 0x19C37D;
    public static final int WARNING = 0xFFA500;
    public static final int ERROR = 0xE03131;

    public static EmbedBuilder info(String title, String desc) {
        return new EmbedBuilder()
                .setColor(INFO)
                .setTitle(title)
                .setDescription(desc);
    }
    public static EmbedBuilder info(String title) {
        return new EmbedBuilder()
                .setColor(INFO)
                .setTitle(title);
    }

    public static EmbedBuilder success(String desc) {
        return new EmbedBuilder()
                .setColor(SUCCESS)
                .setDescription(desc);
    }

    public static EmbedBuilder warning(String title, String desc) {
        return new EmbedBuilder()
                .setColor(WARNING)
                .setTitle(title)
                .setDescription(desc);
    }
    public static EmbedBuilder warning(String title) {
        return new EmbedBuilder()
                .setColor(WARNING)
                .setTitle(title);
    }

    public static EmbedBuilder error(String title, String desc) {
        return new EmbedBuilder()
                .setColor(ERROR)
                .setTitle(title)
                .setDescription(desc);
    }

    public static EmbedBuilder error(String desc) {
        return new EmbedBuilder()
                .setColor(ERROR)
                .setDescription(desc);
    }

}
