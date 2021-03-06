package coco.threshhold.runnables;

import coco.threshhold.Person;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ByOrderRunnable implements Runnable {

    private final ConcurrentMap<Integer, String> resultMap;
    private final Person[] persons;
    private final Comparator<Person> personComparator;
    private final boolean reorderInEachStep;

    public ByOrderRunnable(ConcurrentMap<Integer, String> resultMap, Person[] persons, Comparator<Person> personComparator, boolean reorderInEachStep) {
        this.resultMap = resultMap;
        this.persons = persons;
        this.personComparator = personComparator;
        this.reorderInEachStep = reorderInEachStep;
    }

    @Override
    public void run() {
        int transactions = 0;
        final StringBuilder sb = new StringBuilder();

        // split to those who haven't paid enough (plus) and those, who have overpaid (minus) and get them sorted
        Person[][] plusMinus = splitToPlusAndMinus(persons, personComparator);
        do {
            final Map<Double, Person> plusBalances = Arrays.stream(plusMinus[0])
                    .collect(Collectors.toMap(
                            Person::getBalance,
                            Function.identity(),
                            (person1, person2) -> person1
                    ));
            boolean foundPlusMinus = false;
            for (Person minusP : plusMinus[1]) {
                if (plusBalances.containsKey(-minusP.getBalance())) {
                    Person plusP = plusBalances.get(-minusP.getBalance());
                    doTransaction(plusP, minusP, sb);
                    transactions++;
                    foundPlusMinus = true;
                    break;
                }
            }
            if (foundPlusMinus) {
                plusMinus = splitToPlusAndMinus(persons, personComparator);
            } else {
                final Person plusP = plusMinus[0][0];
                final Person minusP = plusMinus[1][0];
                doTransaction(plusP, minusP, sb);
                transactions ++;
                if (reorderInEachStep) {
                    plusMinus = splitToPlusAndMinus(persons, personComparator);
                } else {
                    if (plusP.getBalance() == 0) {
                        Person[] newArray = new Person[plusMinus[0].length-1];
                        System.arraycopy(plusMinus[0], 1, newArray, 0, plusMinus[0].length-1);
                        plusMinus[0] = newArray;
                    }
                    if (minusP.getBalance() == 0) {
                        Person[] newArray = new Person[plusMinus[1].length-1];
                        System.arraycopy(plusMinus[1], 1, newArray, 0, plusMinus[1].length-1);
                        plusMinus[1] = newArray;
                    }
                }
            }
        } while(plusMinus[0].length > 0 && plusMinus[1].length > 0);

        final String output = ""+transactions+sb.toString();
        /*synchronized (this) {
            System.out.println("Solution found: " + output);
        }*/
        resultMap.computeIfAbsent(transactions, integer -> output);
    }

    private Person[][] splitToPlusAndMinus(Person[] allPersons, Comparator<Person> personComparator) {
        Arrays.sort(allPersons, personComparator); // order array descending by balance
        if (allPersons[0].getBalance() == 0) {
            return new Person[][] {{},{}};
        }
        int i = 0, plusPersons = 0, minusPersons = 0, firstMinusPerson;
        while (allPersons[i].getBalance() > 0) {
            i++; plusPersons++;
        }
        try {
            while (!(allPersons[i].getBalance() < 0)) {
                i++;
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println();
        }
        firstMinusPerson = i;
        minusPersons = allPersons.length - firstMinusPerson;

        final Person[] plus = new Person[plusPersons];
        System.arraycopy(allPersons, 0, plus, 0, plusPersons);
        final Person[] minus = new Person[minusPersons];
        System.arraycopy(allPersons, firstMinusPerson, minus, 0, minusPersons);

        return new Person[][] {plus, minus};
    }

    private void doTransaction(Person plusP, Person minusP, StringBuilder sb) {
        final double transaction = Math.min(plusP.getBalance(), -minusP.getBalance());
        plusP.sendMoney(transaction);
        minusP.acceptMoney(transaction);
        sb.append(String.format("\n%1$s %2$s %3$f", plusP, minusP, transaction));
    }
}
