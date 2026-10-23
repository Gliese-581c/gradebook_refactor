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
public class Gradebook {

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

        String usage = "Usage: java Gradebook COMMAND [ARG]\n"
                + "Commands:\n"
                + "  grades          every student's weighted grade and letter grade\n"
                + "  top N           the N students with the highest grades\n"
                + "  student ID      a breakdown of one student's grade\n"
                + "  assignments     score statistics for each assignment\n"
                + "  distribution    how many students earned each letter grade";

        if (args.length == 0) {
            System.err.println(usage);
            System.exit(1);
        }
        String command = args[0];

        if (command.equals("grades")) {
            if (args.length != 1) {
                System.err.println(usage);
                System.exit(1);
            }
            System.out.printf("%-14s %5s %7s  %s%n", "Name", "ID", "Grade", "Letter");
            for (int s = 0; s < names.length; s++) {
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
                System.out.printf("%-14s %5d %7.2f  %s%n", names[s], ids[s], grade, letterGrade(grade));
            }

        } else if (command.equals("top")) {
            if (args.length != 2) {
                System.err.println(usage);
                System.exit(1);
            }
            int n = 0;
            try {
                n = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Not a number: " + args[1]);
                System.exit(1);
            }
            if (n < 1) {
                System.err.println("N must be at least 1, not " + n);
                System.exit(1);
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

        } else if (command.equals("student")) {
            if (args.length != 2) {
                System.err.println(usage);
                System.exit(1);
            }
            int id = 0;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Not a number: " + args[1]);
                System.exit(1);
            }
            int s = -1;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == id) {
                    s = i;
                }
            }
            if (s == -1) {
                System.err.println("No student with ID " + id);
                System.exit(1);
            }

            System.out.println(names[s] + " (" + ids[s] + ")");
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
                            System.out.printf("  %-8s %4.0f / %3.0f  (dropped)%n", assignNames[a], scores[s][a],
                                    pointsPossible[a]);
                        } else {
                            System.out.printf("  %-8s %4.0f / %3.0f%n", assignNames[a], scores[s][a],
                                    pointsPossible[a]);
                            earned += scores[s][a];
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

        } else if (command.equals("assignments")) {
            if (args.length != 1) {
                System.err.println(usage);
                System.exit(1);
            }
            System.out.printf("%-8s %-9s %8s %7s %5s %5s%n", "Name", "Category", "Possible", "Mean", "Min", "Max");
            for (int a = 0; a < assignNames.length; a++) {
                double total = 0;
                double min = scores[0][a];
                double max = scores[0][a];
                for (int s = 0; s < names.length; s++) {
                    total += scores[s][a];
                    if (scores[s][a] < min) {
                        min = scores[s][a];
                    }
                    if (scores[s][a] > max) {
                        max = scores[s][a];
                    }
                }
                System.out.printf("%-8s %-9s %8.0f %7.2f %5.0f %5.0f%n", assignNames[a], catNames[assignCats[a]],
                        pointsPossible[a], total / names.length, min, max);
            }

        } else if (command.equals("distribution")) {
            if (args.length != 1) {
                System.err.println(usage);
                System.exit(1);
            }
            String[] letters = { "A", "B", "C", "D", "F" };
            int[] counts = new int[letters.length];
            for (int s = 0; s < names.length; s++) {
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

        } else {
            System.err.println("Unknown command: " + command);
            System.err.println(usage);
            System.exit(1);
        }
    }
}
