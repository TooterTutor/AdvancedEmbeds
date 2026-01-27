package io.github.tootertutor.embedbot.util;

import java.awt.Color;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.tootertutor.embedbot.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;

public final class EmbedMapper {
    private static final Logger log = LoggerFactory.getLogger(EmbedMapper.class);

    private EmbedMapper() {
    }

    public static void applyJsonToEmbed(EmbedBuilder builder, DataObject json) {
        // If this is a full message object, extract the first embed
        DataObject embedData = json;
        if (json.hasKey("embeds")) {
            var embeds = json.getArray("embeds");
            if (embeds.length() == 0) {
                throw new IllegalArgumentException("No embeds found in message object");
            }
            embedData = embeds.getObject(0);
        }

        if (embedData.hasKey("title")) {
            String title = embedData.getString("title", null);
            String url = embedData.getString("url", null);
            if (url != null) {
                builder.setTitle(title, url);
            } else {
                builder.setTitle(title);
            }
        }

        if (embedData.hasKey("description")) {
            builder.setDescription(embedData.getString("description"));
        } else if (embedData.hasKey("rawDescription")) {
            builder.setDescription(embedData.getString("rawDescription"));
        }

        if (embedData.hasKey("color")) {
            try {
                Object colorValue = embedData.get("color");
                if (colorValue instanceof Number) {
                    builder.setColor(new Color(((Number) colorValue).intValue()));
                } else if (colorValue instanceof String) {
                    Color parsedColor = parseColorFromString((String) colorValue);
                    if (parsedColor != null) {
                        builder.setColor(parsedColor);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse color: {}", embedData.get("color"), e);
            }
        }

        if (embedData.hasKey("author")) {
            DataObject author = embedData.getObject("author");
            builder.setAuthor(
                    author.getString("name", null),
                    author.getString("url", null),
                    author.getString("icon_url", null));
        }

        if (embedData.hasKey("footer")) {
            DataObject footer = embedData.getObject("footer");
            builder.setFooter(
                    footer.getString("text", null),
                    footer.getString("icon_url", null));
        }

        if (embedData.hasKey("thumbnail")) {
            DataObject thumbnail = embedData.getObject("thumbnail");
            builder.setThumbnail(thumbnail.getString("url", null));
        }

        if (embedData.hasKey("image")) {
            DataObject image = embedData.getObject("image");
            builder.setImage(image.getString("url", null));
        }

        if (embedData.hasKey("timestamp")) {
            try {
                builder.setTimestamp(java.time.Instant.parse(embedData.getString("timestamp")));
            } catch (Exception e) {
                log.warn("Failed to parse timestamp: {}", embedData.get("timestamp"), e);
            }
        }

        if (embedData.hasKey("fields")) {
            var fields = embedData.getArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                DataObject field = fields.getObject(i);
                builder.addField(
                        field.getString("name", ""),
                        field.getString("value", ""),
                        field.getBoolean("inline", false));
            }
        }
    }

    public static Color parseColorFromString(String colorStr) {
        if (colorStr == null || colorStr.isBlank()) {
            return null;
        }

        if (colorStr.startsWith("#")) {
            return Util.parseColor(colorStr);
        }

        if (colorStr.startsWith("hsla(") || colorStr.startsWith("hsl(")) {
            try {
                String values = colorStr.substring(colorStr.indexOf('(') + 1, colorStr.lastIndexOf(')'));

                int firstComma = values.indexOf(',');
                if (firstComma == -1)
                    return null;
                float hue = Float.parseFloat(values.substring(0, firstComma).trim());

                String remainder = values.substring(firstComma + 1);
                String satStr;
                String lightStr;

                if (remainder.trim().startsWith("calc(")) {
                    int calcStart = remainder.indexOf("calc(");
                    int parenCount = 1;
                    int i = calcStart + 5;
                    while (i < remainder.length() && parenCount > 0) {
                        if (remainder.charAt(i) == '(')
                            parenCount++;
                        else if (remainder.charAt(i) == ')')
                            parenCount--;
                        i++;
                    }

                    satStr = remainder.substring(calcStart, i).trim();

                    int starIndex = satStr.indexOf("*");
                    int percentIndex = satStr.indexOf("%");
                    if (starIndex != -1 && percentIndex != -1 && starIndex < percentIndex) {
                        satStr = satStr.substring(starIndex + 1, percentIndex).trim();
                    } else {
                        log.warn("Could not parse calc() saturation from: {}", satStr);
                        return null;
                    }

                    remainder = remainder.substring(i);
                    int nextComma = remainder.indexOf(',');
                    if (nextComma != -1) {
                        lightStr = remainder.substring(nextComma + 1).trim();
                        int lastComma = lightStr.indexOf(',');
                        if (lastComma != -1) {
                            lightStr = lightStr.substring(0, lastComma).trim();
                        }
                    } else {
                        return null;
                    }
                } else {
                    int secondComma = remainder.indexOf(',');
                    if (secondComma == -1)
                        return null;
                    satStr = remainder.substring(0, secondComma).trim();
                    lightStr = remainder.substring(secondComma + 1).trim();

                    int thirdComma = lightStr.indexOf(',');
                    if (thirdComma != -1) {
                        lightStr = lightStr.substring(0, thirdComma).trim();
                    }
                }

                satStr = satStr.replace("%", "").trim();
                lightStr = lightStr.replace("%", "").trim();

                if (satStr.isEmpty() || lightStr.isEmpty()) {
                    log.warn("Empty saturation or lightness in HSL: {} / {}", satStr, lightStr);
                    return null;
                }

                float saturation = Float.parseFloat(satStr) / 100f;
                float lightness = Float.parseFloat(lightStr) / 100f;

                return Color.getHSBColor(hue / 360f, saturation, lightness);
            } catch (Exception e) {
                log.warn("Failed to parse HSL color: {}", colorStr, e);
            }
        }

        return null;
    }

    public static String toHex(Color color) {
        if (color == null) {
            return "";
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static void replaceFields(EmbedBuilder builder, List<MessageEmbed.Field> fields) {
        builder.clearFields();
        fields.forEach(f -> builder.addField(f.getName(), f.getValue(), f.isInline()));
    }
}
