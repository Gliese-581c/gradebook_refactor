
/**
 * Print reports from a course gradebook.
 *
 * Usage: java Gradebook COMMAND [ARG]
 *
 * grades every student's weighted grade and letter grade
 * top N the N students with the highest grades
 * student ID a breakdown of one student's grade
 * assignments score statistics for each assignment
 * distribution how many students earned each letter grade
 *
 * A category's grade is the points earned over the points possible in that
 * category. A category that drops its lowest score leaves out the assignment
 * the student scored the lowest percentage on. The final grade is the
 * weighted sum of the category grades.
 */
import java.util.*;

class category {
    String name;
    double weight;
    boolean drop;
}

class assignment {
    String name;
    int cat;
    double points;
}

class student {
    String name;
    int id;
    double[] scores = new double[9];
}

public class Gradebook {

    private void distribution(ArrayList<student> roster, String[] catNames, boolean[] catDropLowest,
            String[] assignNames,
            int[] assignCats, double[][] scores, double[] pointsPossible, double[] catWeights) {
        String[] letters = { "A", "B", "C", "D", "F" };
        int[] counts = new int[letters.length];
        for (int s = 0; s < roster.size(); s++) {
            double weighted = 0;
            for (int c = 0; c < catNames.length; c++) {
                int dropped = -1;
                if (catDropLowest[c]) {
                    double lowest = Double.MAX_VALUE;
                    for (int a = 0; a < assignNames.length; a++) {
                        if (assignCats[a] == c && scores[s][a] / pointsPossible[a] < lowest) {
                            lowest = scores[s][a] / pointsPossible[a];
                            dropped = a;
                        }
                    }
                }
                double earned = 0;
                double possible = 0;
                for (int a = 0; a < assignNames.length; a++) {
                    if (assignCats[a] == c && a != dropped) {
                        earned += scores[s][a];
                        possible += pointsPossible[a];
                    }
                }
                weighted += catWeights[c] * (earned / possible);
            }
            double grade = weighted * 100;
            if (grade >= 90) {
                counts[0]++;
            } else if (grade >= 80) {
                counts[1]++;
            } else if (grade >= 70) {
                counts[2]++;
            } else if (grade >= 60) {
                counts[3]++;
            } else {
                counts[4]++;
            }
        }
        for (int i = 0; i < letters.length; i++) {
            System.out.printf("%s %2d  %s%n", letters[i], counts[i], "*".repeat(counts[i]));
        }
    }

    private void assignments(ArrayList<assignment> assignments, ArrayList<student> roster,
            ArrayList<category> categories) {
        System.out.printf("%-8s %-9s %8s %7s %5s %5s%n", "Name", "Category", "Possible", "Mean", "Min", "Max");
        for (int a = 0; a < assignments.size(); a++) {
            double total = 0;
            double min = roster.get(0).scores[a];
            double max = roster.get(0).scores[a];
            for (int s = 0; s < roster.size(); s++) {
                total += roster.get(s).scores[a];
                if (roster.get(s).scores[a] < min) {
                    min = roster.get(s).scores[a];
                }
                if (roster.get(s).scores[a] > max) {
                    max = roster.get(s).scores[a];
                }
            }
            System.out.printf("%-8s %-9s %8.0f %7.2f %5.0f %5.0f%n", assignments.get(a).name,
                    categories.get(assignments.get(a).cat).name,
                    assignments.get(a).points, total / roster.size(), min, max);
        }
    }

    private void student(String args, ArrayList<student> roster, String[] catNames, boolean[] catDropLowest,
            String[] assignNames, int[] assignCats, double[] pointsPossible, double[] catWeights) {
        int id = 0;
        try {
            id = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            error(args, 1);
        }
        int s = -1;
        for (int i = 0; i < roster.size(); i++) {
            if (roster.get(i).id == id) {
                s = i;
            }
        }
        if (s == -1) {
            System.err.println("No student with ID " + id);
            System.exit(1);
        }
        System.out.println(roster.get(s).name + " (" + roster.get(s).id + ")");
        double weighted = 0;
        for (int c = 0; c < catNames.length; c++) {
            int dropped = -1;
            if (catDropLowest[c]) {
                double lowest = Double.MAX_VALUE;
                for (int a = 0; a < assignNames.length; a++) {
                    if (assignCats[a] == c && roster.get(s).scores[a] / pointsPossible[a] < lowest) {
                        lowest = roster.get(s).scores[a] / pointsPossible[a];
                        dropped = a;
                    }
                }
            }
            System.out.println();
            if (catDropLowest[c]) {
                System.out.printf("%s (%.0f%%, lowest score dropped)%n", catNames[c], catWeights[c] * 100);
            } else {
                System.out.printf("%s (%.0f%%)%n", catNames[c], catWeights[c] * 100);
            }
            double earned = 0;
            double possible = 0;
            for (int a = 0; a < assignNames.length; a++) {
                if (assignCats[a] == c) {
                    if (a == dropped) {
                        System.out.printf("  %-8s %4.0f / %3.0f  (dropped)%n", assignNames[a], roster.get(s).scores[a],
                                pointsPossible[a]);
                    } else {
                        System.out.printf("  %-8s %4.0f / %3.0f%n", assignNames[a], roster.get(s).scores[a],
                                pointsPossible[a]);
                        earned += roster.get(s).scores[a];
                        possible += pointsPossible[a];
                    }
                }
            }
            System.out.printf("  %-8s %10.2f%%%n", "Total", earned / possible * 100);
            weighted += catWeights[c] * (earned / possible);
        }
        double grade = weighted * 100;
        System.out.println();
        System.out.printf("Final grade: %.2f (%s)%n", grade, letterGrade(grade));
    }

    private void top(String args, int[] ids, String[] names, String[] catNames, String[] assignNames, int[] assignCats,
            double[][] scores, double[] pointsPossible, boolean[] catDropLowest, double[] catWeights) {

        int n = 0;
        try {
            n = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            error(args, 1);
        }
        if (n < 1) {
            error(args, 2);
        }

        double[] grades = new double[names.length];
        for (int s = 0; s < names.length; s++) {
            for (int c = 0; c < catNames.length; c++) {
                double earned = 0;
                double possible = 0;
                double lowestPct = Double.MAX_VALUE;
                double lowestEarned = 0;
                double lowestPossible = 0;
                for (int a = 0; a < assignNames.length; a++) {
                    if (assignCats[a] == c) {
                        earned += scores[s][a];
                        possible += pointsPossible[a];
                        if (scores[s][a] / pointsPossible[a] < lowestPct) {
                            lowestPct = scores[s][a] / pointsPossible[a];
                            lowestEarned = scores[s][a];
                            lowestPossible = pointsPossible[a];
                        }
                    }
                }
                if (catDropLowest[c]) {
                    earned -= lowestEarned;
                    possible -= lowestPossible;
                }
                grades[s] += catWeights[c] * (earned / possible);
            }
            grades[s] = grades[s] * 100;
        }

        // sort everything by grade, highest first. This is
        // selection sort. It sure would be nice to be able to
        // use Arrays.sort or Collections.sort, but we can't
        // do that due to the way the data is currently structured
        // (HINT HINT)
        for (int i = 0; i < names.length - 1; i++) {
            int best = i;
            for (int j = i + 1; j < names.length; j++) {
                if (grades[j] > grades[best]) {
                    best = j;
                }
            }
            String tempName = names[i];
            names[i] = names[best];
            names[best] = tempName;
            int tempId = ids[i];
            ids[i] = ids[best];
            ids[best] = tempId;
            double[] tempScores = scores[i];
            scores[i] = scores[best];
            scores[best] = tempScores;
            double tempGrade = grades[i];
            grades[i] = grades[best];
            grades[best] = tempGrade;
        }

        System.out.printf("%4s  %-14s %5s %7s  %s%n", "Rank", "Name", "ID", "Grade", "Letter");
        for (int i = 0; i < n && i < names.length; i++) {
            System.out.printf("%4d  %-14s %5d %7.2f  %s%n", i + 1, names[i], ids[i], grades[i],
                    letterGrade(grades[i]));
        }
    }

    private void grades(ArrayList<student> roster, String[] catNames, boolean[] catDropLowest, int[] assignCats,
            double[] pointsPossible, double[] catWeights, String[] assignNames) {
        System.out.printf("%-14s %5s %7s  %s%n", "Name", "ID", "Grade", "Letter");
        for (int s = 0; s < roster.size(); s++) {
            double weighted = 0;
            for (int c = 0; c < catNames.length; c++) {
                int dropped = -1;
                if (catDropLowest[c]) {
                    double lowest = Double.MAX_VALUE;
                    for (int a = 0; a < assignNames.length; a++) {
                        if (assignCats[a] == c && roster.get(s).scores[a] / pointsPossible[a] < lowest) {
                            lowest = roster.get(s).scores[a] / pointsPossible[a];
                            dropped = a;
                        }
                    }
                }
                double earned = 0;
                double possible = 0;
                for (int a = 0; a < assignNames.length; a++) {
                    if (assignCats[a] == c && a != dropped) {
                        earned += roster.get(s).scores[a];
                        possible += pointsPossible[a];
                    }
                }
                weighted += catWeights[c] * (earned / possible);
            }
            double grade = weighted * 100;
            System.out.printf("%-14s %5d %7.2f  %s%n", roster.get(s).name, roster.get(s).id, grade, letterGrade(grade));
        }
    }

    private void error(String output, int option) {
        switch (option) {
            case 0:
                System.err.println(output);
                System.exit(1);
            case 1:
                System.err.println("Not a number: " + output);
                System.exit(1);
            case 2:
                System.err.println("N must be at least 1, not " + output);
                System.exit(1);
        }

    }

    private String letterGrade(double grade) {
        String letter;
        if (grade >= 90) {
            letter = "A";
        } else if (grade >= 80) {
            letter = "B";
        } else if (grade >= 70) {
            letter = "C";
        } else if (grade >= 60) {
            letter = "D";
        } else {
            letter = "F";
        }
        return letter;
    }

    public static void main(String[] args) {
        new Gradebook().run(args);
    }

    private void run(String[] args) {
        // The gradebook data is hard-coded into the class. This isn't
        // ideal practice in real life, but for the purposes of this
        // assignment, it's a lot simpler than loading them from a CSV
        // file or similar.

        // Assignment category names
        String[] catNames = { "Homework", "Labs", "Exams" };
        // Assignment category weights
        double[] catWeights = { 0.30, 0.30, 0.40 };
        // Whether or not we drop the lowest score in each assignment group
        boolean[] catDropLowest = { true, false, false };

        // Names of the assignments
        String[] assignNames = { "HW1", "HW2", "HW3", "HW4", "Lab1", "Lab2", "Lab3", "Midterm", "Final" };
        // Which category each assignment is in
        int[] assignCats = { 0, 0, 0, 0, 1, 1, 1, 2, 2 };
        double[] pointsPossible = { 10, 10, 20, 10, 20, 25, 30, 100, 150 };

        // Students in the class
        String[] names = { "Ada Okafor", "Ben Castillo", "Chloe Nguyen", "Dev Raman", "Elena Petrov", "Farid Haddad",
                "Grace Liu", "Hugo Martins" };
        // Corresponding student ids
        int[] ids = { 1041, 1057, 1063, 1072, 1088, 1093, 1105, 1118 };

        // Scores. Each row is one student (in the order of the students above)
        double[][] scores = {
                { 10, 9, 19, 10, 19, 24, 29, 94, 141 },
                { 8, 7, 15, 0, 17, 20, 25, 81, 118 },
                { 9, 10, 18, 9, 20, 23, 28, 88, 135 },
                { 6, 7, 12, 8, 15, 18, 22, 72, 104 },
                { 7, 0, 14, 8, 16, 19, 21, 65, 95 },
                { 10, 8, 17, 9, 18, 22, 26, 79, 121 },
                { 5, 6, 10, 4, 12, 14, 16, 55, 80 },
                { 9, 9, 16, 7, 17, 21, 24, 70, 110 },
        };

        ArrayList<category> categories = new ArrayList<category>();
        for (int i = 0; i < catNames.length; i++) {
            category temp = new category();
            temp.name = catNames[i];
            temp.weight = catWeights[i];
            temp.drop = catDropLowest[i];
        }

        ArrayList<assignment> assignments = new ArrayList<assignment>();
        for (int i = 0; i < assignNames.length; i++) {
            assignment temp = new assignment();
            temp.name = assignNames[i];
            temp.cat = assignCats[i];
            temp.points = pointsPossible[i];
        }

        ArrayList<student> roster = new ArrayList<student>();
        for (int i = 0; i < names.length; i++) {
            student temp = new student();
            temp.name = names[i];
            temp.id = ids[i];
            for (int j = 0; j < scores[i].length; j++) {
                temp.scores[j] = scores[i][j];
            }
            roster.add(temp);
        }

        String usage = "Usage: java Gradebook COMMAND [ARG]\n"
                + "Commands:\n"
                + "  grades          every student's weighted grade and letter grade\n"
                + "  top N           the N students with the highest grades\n"
                + "  student ID      a breakdown of one student's grade\n"
                + "  assignments     score statistics for each assignment\n"
                + "  distribution    how many students earned each letter grade";

        if (args.length == 0) {
            error(usage, 0);
        }
        switch (args[0]) {
            case "grades":
                if (args.length > 1) {
                    error(usage, 0);
                } else {
                    grades(roster, catNames, catDropLowest, assignCats, pointsPossible, catWeights,
                            assignNames);
                }
                break;
            case "top":
                if (args.length != 2) {
                    error(usage, 0);
                } else {
                    top(args[1], ids, names, catNames, assignNames, assignCats, scores, pointsPossible, catDropLowest,
                            catWeights);
                }
                break;
            case "student":
                if (args.length != 2) {
                    error(usage, 0);
                } else {
                    student(args[1], roster, catNames, catDropLowest, assignNames, assignCats,
                            pointsPossible, catWeights);
                }
                break;
            case "assignments":
                if (args.length != 1) {
                    error(usage, 0);
                } else {
                    assignments(assignments, roster, categories);
                }
                break;
            case "distribution":
                if (args.length != 1) {
                    error(usage, 0);
                } else {
                    distribution(roster, catNames, catDropLowest, assignNames, assignCats, scores, pointsPossible,
                            catWeights);
                }
                break;
            default:
                System.err.println("Unknown command: " + args[0]);
                error(usage, 0);
        }
        System.exit(0);
    }
}