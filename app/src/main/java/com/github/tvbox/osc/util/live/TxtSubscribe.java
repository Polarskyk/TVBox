package com.github.tvbox.osc.util.live;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtSubscribe {
    private static final Pattern NAME_PATTERN = Pattern.compile(".*,(.+?)$");
    private static final Pattern GROUP_PATTERN = Pattern.compile("group-title=\"?(.+?)\"?([,;]|$)");

    public static void parse(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        if (str == null) return;
        // Handle UTF-8 BOM
        if (str.startsWith("\ufeff")) {
            str = str.substring(1);
        }
        String probe = str.trim();
        if (probe.startsWith("#EXTM3U")) {
            parseM3u(linkedHashMap, str);
        } else {
            parseTxt(linkedHashMap, str);
        }
    }

    //解析m3u后缀
    private static void parseM3u(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String line;
            String pendingGroup = null;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#EXTM3U")) continue;
                if (isSetting(line)) continue;

                // Some m3u uses #EXTGRP:xxx as group hint
                if (line.startsWith("#EXTGRP:")) {
                    String grp = line.substring("#EXTGRP:".length()).trim();
                    if (!grp.isEmpty()) pendingGroup = grp;
                    continue;
                }

                if (line.startsWith("#EXTINF") || line.contains("#EXTINF")) {
                    String name = getStrByRegex(NAME_PATTERN, line);
                    String group = getStrByRegex(GROUP_PATTERN, line);
                    if ("未分组".equals(group) && pendingGroup != null && !pendingGroup.isEmpty()) {
                        group = pendingGroup;
                    }

                    // Next non-empty, non-tag line should be the url.
                    String url = null;
                    while ((url = bufferedReader.readLine()) != null) {
                        url = url.trim();
                        if (url.isEmpty()) continue;
                        if (isSetting(url)) continue;
                        if (url.startsWith("#EXTGRP:")) {
                            String grp = url.substring("#EXTGRP:".length()).trim();
                            if (!grp.isEmpty()) pendingGroup = grp;
                            continue;
                        }
                        // Skip other tag lines between EXTINF and URL
                        if (url.startsWith("#")) continue;
                        break;
                    }

                    if (url != null && isUrl(url)) {
                        LinkedHashMap<String, ArrayList<String>> channelMap;
                        if (linkedHashMap.containsKey(group)) {
                            channelMap = linkedHashMap.get(group);
                        } else {
                            channelMap = new LinkedHashMap<>();
                            linkedHashMap.put(group, channelMap);
                        }
                        ArrayList<String> urls;
                        if (channelMap.containsKey(name)) {
                            urls = channelMap.get(name);
                        } else {
                            urls = new ArrayList<>();
                            channelMap.put(name, urls);
                        }
                        if (!urls.contains(url)) urls.add(url);
                    }

                    // Reset EXTGRP hint after consuming one channel
                    pendingGroup = null;
                }
            }
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String getStrByRegex(Pattern pattern, String line) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) return matcher.group(1);
        return pattern.pattern().equals(GROUP_PATTERN.pattern()) ? "未分组" : "未命名";
    }

    private static boolean isUrl(String url) {
        if (url.isEmpty()) return false;
        if (url.startsWith("http") || url.startsWith("rtp") || url.startsWith("rtsp") || url.startsWith("rtmp") || url.startsWith("udp") || url.startsWith("mitv") || url.startsWith("p2p") || url.startsWith("tvbus")) return true;
        return url.contains("://");
    }

    private static boolean isSetting(String line) {
        return line.startsWith("ua") || line.startsWith("parse") || line.startsWith("click") || line.startsWith("player") || line.startsWith("header") || line.startsWith("format") || line.startsWith("origin") || line.startsWith("referer") || line.startsWith("#EXTHTTP:") || line.startsWith("#EXTVLCOPT:") || line.startsWith("#KODIPROP:");
    }

    //解析txt后缀
    public static void parseTxt(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap, String str) {
        ArrayList<String> arrayList;
        try {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(str));
            String readLine = bufferedReader.readLine();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = new LinkedHashMap<>();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap3 = linkedHashMap2;
            while (readLine != null) {
                if (readLine.trim().isEmpty() || readLine.startsWith("#")) {
                    readLine = bufferedReader.readLine();
                } else {
                    String[] split = readLine.split(",");
                    if (split.length < 2) {
                        readLine = bufferedReader.readLine();
                    } else {
                        if (readLine.contains("#genre#")) {
                            String trim = split[0].trim();
                            if (!linkedHashMap.containsKey(trim)) {
                                linkedHashMap3 = new LinkedHashMap<>();
                                linkedHashMap.put(trim, linkedHashMap3);
                            } else {
                                linkedHashMap3 = linkedHashMap.get(trim);
                            }
                        } else {
                            String trim2 = split[0].trim();
                            for (String str2 : split[1].trim().split("#")) {
                                String trim3 = str2.trim();
                                if (isUrl(trim3)) {
                                    if (!linkedHashMap3.containsKey(trim2)) {
                                        arrayList = new ArrayList<>();
                                        linkedHashMap3.put(trim2, arrayList);
                                    } else {
                                        arrayList = linkedHashMap3.get(trim2);
                                    }
                                    if (!arrayList.contains(trim3)) {
                                        arrayList.add(trim3);
                                    }
                                }
                            }
                        }
                        readLine = bufferedReader.readLine();
                    }
                }
            }
            bufferedReader.close();
            if (linkedHashMap2.isEmpty()) {
                return;
            }
            linkedHashMap.put("未分组", linkedHashMap2);
        } catch (Throwable unused) {
        }
    }

    public static JsonArray live2JsonArray(LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> linkedHashMap) {
        JsonArray jsonarr = new JsonArray();
        for (String str : linkedHashMap.keySet()) {
            JsonArray jsonarr2 = new JsonArray();
            LinkedHashMap<String, ArrayList<String>> linkedHashMap2 = linkedHashMap.get(str);
            if (!linkedHashMap2.isEmpty()) {
                for (String str2 : linkedHashMap2.keySet()) {
                    ArrayList<String> arrayList = linkedHashMap2.get(str2);
                    if (!arrayList.isEmpty()) {
                        JsonArray jsonarr3 = new JsonArray();
                        for (int i = 0; i < arrayList.size(); i++) {
                            jsonarr3.add(arrayList.get(i));
                        }
                        JsonObject jsonobj = new JsonObject();
                        try {
                            jsonobj.addProperty("name", str2);
                            jsonobj.add("urls", jsonarr3);
                        } catch (Throwable e) {
                        }
                        jsonarr2.add(jsonobj);
                    }
                }
                JsonObject jsonobj2 = new JsonObject();
                try {
                    jsonobj2.addProperty("group", str);
                    jsonobj2.add("channels", jsonarr2);
                } catch (Throwable e) {
                }
                jsonarr.add(jsonobj2);
            }
        }
        return jsonarr;
    }
}
