// file: Filter.java
package xyz.n501yhappy.happyfilter.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.n501yhappy.happyfilter.utils.structs.Area;
import xyz.n501yhappy.happyfilter.utils.structs.Filtered;

public class Filter {
    private AhoCorasick AhoC = new AhoCorasick();
    private List<String> filterList = new ArrayList<>();
    private Map<String,Pattern> patternCache = new HashMap<>();
    public void buildAC(List<String> filters) {
        this.filterList = new ArrayList<>(filters);
        this.AhoC = new AhoCorasick();
        for (String filter : filters) AhoC.insert(filter);
        AhoC.build();
    }

    public Filtered filterText(String message, List<String> filters) {
        if (!isFilterListEqual(this.filterList, filters)) buildAC(filters);

        List<Area> areas = new ArrayList<>();
        List<AhoCorasick.Hit> hits = AhoC.search(message);

        for (AhoCorasick.Hit hit : hits) {//[)
            areas.add(new Area(hit.start, hit.end));
        }
        Filtered result = new Filtered(areas, !hits.isEmpty());
        result.clearCoverd();
        return result;
    }

    public Filtered filterRegex(String message, List<String> regexPatterns) {
        if (regexPatterns == null || regexPatterns.isEmpty() || message.isEmpty()) {
            return new Filtered(List.of(), false);
        }

        List<Area> areas = new ArrayList<>();

        for (String regex : regexPatterns) {
            Pattern p = patternCache.computeIfAbsent(regex, Pattern::compile);
            Matcher m = p.matcher(message);

            while (m.find()) {
                areas.add(new Area(m.start(), m.end()));
            }
        }
        Filtered result = new Filtered(areas, !areas.isEmpty());
        result.clearCoverd();

        return result;
    }

    private boolean isFilterListEqual(List<String> list1, List<String> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        List<String> sortedList1 = new ArrayList<>(list1);
        List<String> sortedList2 = new ArrayList<>(list2);
        Collections.sort(sortedList1);
        Collections.sort(sortedList2);

        return sortedList1.equals(sortedList2);
    }
}
