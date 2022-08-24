package fr.kaname.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {

    private Configuration config;

    private String prefix = "[KanaMessageManagerAPI] ";
    private TextColor prefixColor = TextColor.color(0x00FFFF);
    private TextColor defaultColor = TextColor.color(0xFFFFFF);

    private final Map<String, Integer> ColorMap = new HashMap<>();
    private final Map<String, TextComponent> MessageMap = new HashMap<>();

    public MessageManager(Configuration config) {

        this.console(MessageManager.createBasicTextComponent("Init MessageManager..."));

        this.config = config;
        this.MessageMap.put("ErrorKeyNotFound", Component.text()
                .append(MessageManager.createBasicTextComponent("Message key {key} not found", 0xff0000))
                .build());

        String PrefixColor = this.config.getString("PluginPrefixColor");
        this.prefix = this.config.getString("PluginPrefix");

        if (PrefixColor != null) {
            this.prefixColor = TextColor.color(Integer.parseInt(PrefixColor, 16));
        }

        ConfigurationSection colorsConfigurationSection = this.config.getConfigurationSection("Colors");
        ConfigurationSection messageConfigurationSection = this.config.getConfigurationSection("Messages");

        if (colorsConfigurationSection != null) {
            for (String ColorKey : colorsConfigurationSection.getKeys(true)) {
                this.ColorMap.put(ColorKey, Integer.parseInt(Objects.requireNonNull(colorsConfigurationSection.getString(ColorKey)), 16));
            }
        }

        if (messageConfigurationSection != null) {
            for (String MessageKey : messageConfigurationSection.getKeys(true)) {
                String msg = messageConfigurationSection.getString(MessageKey);
                if (msg != null) {
                    this.MessageMap.put(MessageKey, this.formatMessage(msg));
                }
            }
        }

        this.console("InitDone");
    }

    public void console(String key) {
        this.console(this.getTextCompoenntByMessageKey(key));
    }

    public void console(TextComponent message) {
        TextComponent log = Component.text().content(this.prefix).color(this.prefixColor).append(message).build();
        Bukkit.getConsoleSender().sendMessage(log);

    }

    private TextComponent getTextCompoenntByMessageKey(String key) {
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder().matchLiteral("[key]").replacement(key).build();
        if (!this.MessageMap.containsKey(key)) {
            key = "ErrorKeyNotFound";
        }
        return (TextComponent) this.MessageMap.get(key).replaceText(replacementConfig);
    }

    public static @NotNull TextComponent createBasicTextComponent(@NotNull String message) {
        return MessageManager.createBasicTextComponent(message, 0xFFFFFF);
    }

    public static @NotNull TextComponent createBasicTextComponent(@NotNull String message, @NotNull Integer color) {
        return Component.text(message).color(TextColor.color(color));
    }

    public @NotNull Integer getConfigColor(@NotNull String key) {
        return this.ColorMap.getOrDefault(key, this.defaultColor.value());
    }

    private TextComponent formatMessage(@NotNull String message) {
        Pattern pattern = Pattern.compile("[{]\\w+[}]");
        Pattern patternText = Pattern.compile("[^}]+[{]");

        message = message + "{";

        Matcher match = pattern.matcher(message);
        Matcher matchText = patternText.matcher(message);

        TextComponent.Builder textComponent = Component.text();

        Map<Integer, Map<String, String>> messageOrder = new HashMap<>();
        List<Integer> index = new ArrayList<>();

        Map<String, String> messageMap;

        while (matchText.find()) {
            String msg = message.substring(matchText.start(), matchText.end()).replace("{", "").replace("}", "");
            messageMap = new HashMap<>();
            messageMap.put("type", "text");
            messageMap.put("content", msg);
            messageOrder.put(matchText.start(), messageMap);
            index.add(matchText.start());
        }

        while (match.find()) {
            String msg = message.substring(match.start(), match.end()).replace("{", "").replace("}", "");
            messageMap = new HashMap<>();
            messageMap.put("type", "color");
            messageMap.put("content", msg);
            messageOrder.put(match.start(), messageMap);
            index.add(match.start());
        }

        Collections.sort(index);
        int Color = this.defaultColor.value();
        for (int i : index) {

            Map<String, String> map = messageOrder.get(i);
            String type = map.get("type");
            String content = map.get("content");

            if (type.equals("text")) {
                textComponent.append(MessageManager.createBasicTextComponent(content, Color));
            } else if (type.equals("color")) {
                Color = this.getConfigColor(content);
            }
        }

        return textComponent.build();

    }

}
