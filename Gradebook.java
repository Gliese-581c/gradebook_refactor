
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
    String catName;
    int cat;
    double points;
}

class student {
    String name;
    int id;
    double[] scores = new double[9];
    double total = 0;
}

public class Gradebook {

    private final String[] letters = { "A", "B", "C", "D", "F" };
    private int[] counts = new int[letters.length];
    private static ArrayList<student> students;
    private static ArrayList<assignment> assignments;
    private static ArrayList<category> categories;

    private void distribution() {
        for (int s = 0; s < students.size(); s++) {
            double grade = weighting(s) * 100;
            Grade(grade);
        }
        for (int i = 0; i < letters.length; i++) {
            System.out.printf("%s %2d  %s%n", letters[i], counts[i], "*".repeat(counts[i]));
        }
    }

    private void assignments() {
        System.out.printf("%-8s %-9s %8s %7s %5s %5s%n", "Name", "Category", "Possible", "Mean", "Min", "Max");
        for (int a = 0; a < assignments.size(); a++) {
            double min = getMin(a);
            double max = getMax(a);
            double mean = getMean(a, students.size());
            System.out.printf("%-8s %-9s %8.0f %7.2f %5.0f %5.0f%n", getAssignmentName(a), getCatAss(a), getPoints(a),
                    mean, min, max);
        }
    }

    private void student(String args) {
        int s = checkId(args);
        System.out.println(getName(s) + " (" + getId(s) + ")");
        for (int c = 0; c < categories.size(); c++) {
            int dropped = findDrop(c, s);
            if (getDrop(c)) {
                System.out.printf("\n%s (%.0f%%, lowest score dropped)%n", getCatName(c), getWeight(c) * 100);
            } else {
                System.out.printf("\n%s (%.0f%%)%n", getCatName(c), getWeight(c) * 100);
            }
            System.out.printf("  %-8s %10.2f%%%n", "Total", total(s, c, dropped) * 100);
        }
        double grade = weighting(s) * 100;
        System.out.printf("\nFinal grade: %.2f (%s)%n", grade, Grade(grade));
    }

    private void top(String args) {

        int n = parse(args);
        if (n < 1) {
            error(args, 2);
        }

        for (int s = 0; s < students.size(); s++) {
            students.get(s).total = calculateTotal(s) * 100;
        }

        Collections.sort(students, (a, b) -> Double.compare(a.total, b.total));
        Collections.reverse(students);

        System.out.printf("%4s  %-14s %5s %7s  %s%n", "Rank", "Name", "ID", "Grade", "Letter");
        for (int i = 0; i < n && i < students.size(); i++) {
            System.out.printf("%4d  %-14s %5d %7.2f  %s%n", i + 1, getName(i), getId(i),
                    getTotal(i), Grade(getTotal(i)));
        }
    }

    private double calculateTotal(int s) {
        double total = 0;
        for (int c = 0; c < categories.size(); c++) {

            double earned = 0;
            double possible = 0;
            double lowestPct = Double.MAX_VALUE;
            double lowestEarned = 0;
            double lowestPossible = 0;
            for (int a = 0; a < assignments.size(); a++) {
                if (assignments.get(a).cat == c) {
                    earned += getScore(s, a);
                    possible += assignments.get(a).points;
                    if (getScore(s, a) / getPoints(a) < lowestPct) {
                        lowestPct = getScore(s, a) / getPoints(a);
                        lowestEarned = getScore(s, a);
                        lowestPossible = getPoints(a);
                    }
                }
            }
            if (getDrop(c)) {
                earned -= lowestEarned;
                possible -= lowestPossible;
            }
            total += getWeight(c) * (earned / possible);
        }
        return total;
    }

    private void grades() {
        System.out.printf("%-14s %5s %7s  %s%n", "Name", "ID", "Grade", "Letter");

        for (int s = 0; s < students.size(); s++) {
            double grade = weighting(s) * 100;
            System.out.printf("%-14s %5d %7.2f  %s%n", getName(s), getId(s), grade,
                    Grade(grade));
        }
    }

    private int findDrop(int c, int s) {
        int dropped = 0;
        if (getDrop(c)) {
            double lowest = Double.MAX_VALUE;
            for (int a = 0; a < assignments.size(); a++) {
                if (getCat(a) == c && getScore(s, a) / getPoints(a) < lowest) {
                    lowest = getScore(s, a) / getPoints(a);
                    dropped = a;
                }
            }
        }
        return dropped;
    }

    private double total(int s, int c, int dropped) {
        double earned = 0;
        double possible = 0;
        for (int a = 0; a < assignments.size(); a++) {
            if (getCat(a) == c) {
                if (a == dropped) {
                    System.out.printf("  %-8s %4.0f / %3.0f  (dropped)%n", getAssignmentName(a),
                            getScore(s, a),
                            getPoints(a));
                } else {
                    System.out.printf("  %-8s %4.0f / %3.0f%n", getAssignmentName(a), getScore(s, a),
                            getPoints(a));
                    earned += getScore(s, a);
                    possible += getPoints(a);
                }
            }
        }
        return earned / possible;
    }

    private int checkId(String args) {
        int id = parse(args);
        int s = -1;
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).id == id) {
                s = i;
                break;
            }
        }
        if (s == -1) {
            error(Integer.toString(id), 3);
        }
        return s;
    }

    private double weighting(int s) {
        double weighted = 0;
        for (int c = 0; c < categories.size(); c++) {
            int dropped = -1;
            if (getDrop(c)) {
                double lowest = Double.MAX_VALUE;
                for (int a = 0; a < assignments.size(); a++) {
                    if (getCat(a) == c
                            && getScore(s, a) / getPoints(a) < lowest) {
                        lowest = getScore(s, a) / getPoints(a);
                        dropped = a;
                    }
                }
            }
            double earned = 0;
            double possible = 0;
            for (int a = 0; a < assignments.size(); a++) {
                if (getCat(a) == c && a != dropped) {
                    earned += getScore(s, a);
                    possible += getPoints(a);
                }
            }
            weighted += getWeight(c) * (earned / possible);
        }
        return weighted;
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
            case 3:
                System.err.println("No student with ID " + output);
                System.exit(1);
        }

    }

    private int parse(String args) {
        int n = 0;
        try {
            n = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            error(args, 1);
        }
        return n;
    }

    private String Grade(double grade) {
        if (grade >= 90) {
            counts[0]++;
            return letters[0];
        } else if (grade >= 80) {
            counts[1]++;
            return letters[1];
        } else if (grade >= 70) {
            counts[2]++;
            return letters[2];
        } else if (grade >= 60) {
            counts[3]++;
            return letters[3];
        } else {
            counts[4]++;
            return letters[4];
        }
    }

    private double getWeight(int c) {
        return categories.get(c).weight;
    }

    private boolean getDrop(int c) {
        return categories.get(c).drop;
    }

    private String getCatName(int c) {
        return categories.get(c).name;
    }

    private String getAssignmentName(int a) {
        return assignments.get(a).name;
    }

    private int getCat(int a) {
        return assignments.get(a).cat;
    }

    private String getCatAss(int a) {
        return categories.get(assignments.get(a).cat).name;
    }

    private double getPoints(int a) {
        ArrayList<assignment> temp = new ArrayList<assignment>(assignments);
        return temp.get(a).points;
    }

    private String getName(int s) {
        return students.get(s).name;
    }

    private int getId(int s) {
        return students.get(s).id;
    }

    private double getScore(int s, int i) {
        return students.get(s).scores[i];
    }

    private double getTotal(int s) {
        return students.get(s).total;
    }

    private double getMin(int a) {
        double min = Integer.MAX_VALUE;
        for (int i = 0; i < students.size(); i++) {
            if (getScore(i, a) < min) {
                min = getScore(i, a);
            }
        }
        return min;
    }

    private double getMax(int a) {
        double max = Integer.MIN_VALUE;
        for (int i = 0; i < students.size(); i++) {
            if (getScore(i, a) > max) {
                max = getScore(i, a);
            }
        }
        return max;
    }

    private double getMean(int a, int div) {
        double mean = 0;
        for (int i = 0; i < students.size(); i++) {
            mean += getScore(i, a);
        }
        mean = mean / div;
        return mean;
    }

    private static void buildStudents(String[] names, int[] ids, double[][] scores) {
        ArrayList<student> tempList = new ArrayList<student>();
        for (int i = 0; i < names.length; i++) {
            int tempScore = 0;
            student temp = new student();
            temp.name = names[i];
            temp.id = ids[i];
            for (int j = 0; j < scores[i].length; j++) {
                tempScore += scores[i][j];
                temp.scores[j] = scores[i][j];
            }
            temp.total = tempScore;
            tempList.add(temp);
        }
        students = tempList;
    }

    private static void buildAssignments(String[] assignNames, int[] assignCats,
            double[] pointsPossible) {
        ArrayList<assignment> tempAss = new ArrayList<assignment>();
        for (int i = 0; i < assignNames.length; i++) {
            assignment temp = new assignment();
            temp.name = assignNames[i];
            temp.cat = assignCats[i];
            temp.points = pointsPossible[i];
            tempAss.add(temp);
        }
        assignments = tempAss;
    }

    private static void buildCats(String[] catNames, double[] catWeights, boolean[] catDropLowest) {
        ArrayList<category> tempCat = new ArrayList<category>();
        for (int i = 0; i < catNames.length; i++) {
            category temp = new category();
            temp.name = catNames[i];
            temp.weight = catWeights[i];
            temp.drop = catDropLowest[i];
            tempCat.add(temp);
        }
        categories = tempCat;
    }

    public static void main(String[] args) {
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

        buildStudents(names, ids, scores);
        buildAssignments(assignNames, assignCats, pointsPossible);
        buildCats(catNames, catWeights, catDropLowest);

        new Gradebook().run(args);
    }

    private void run(String[] args) {

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
                    grades();
                }
                break;
            case "top":
                if (args.length != 2) {
                    error(usage, 0);
                } else {
                    top(args[1]);
                }
                break;
            case "student":
                if (args.length != 2) {
                    error(usage, 0);
                } else {
                    student(args[1]);
                }
                break;
            case "assignments":
                if (args.length != 1) {
                    error(usage, 0);
                } else {
                    assignments();
                }
                break;
            case "distribution":
                if (args.length != 1) {
                    error(usage, 0);
                } else {
                    distribution();
                }
                break;
            default:
                System.err.println("Unknown command: " + args[0]);
                error(usage, 0);
        }
        System.exit(0);
    }
}