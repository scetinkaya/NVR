

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Ant {

    private int startPeak, curPeak, assignedAACount,
                number,              // ant's number. used without reason. prosto tak
                candSize,            // will display the candidate list search point (if 25% and peak=80 will be =20 then +1, +1, ...)
                availableAA[],       // availability list of AA's ; "1=available"; "0=infeasible"; "-1=unavailable";
                peakConstraints[],   // peak constraint count list (for most constraint peak selection)
                peakMemoryReverse[]; // reverse array of peakMemory
    public int curStep, peakMemory[];// memory of the ant. peaks which are assigned sequentially

    public Assignment peakAssignment[];           // assignment of peak to AA with Feasibility; "i = peak"
    public boolean stopWorking = false;
    private final boolean infeasibleSelect,       // whether noe infeasible AA's will be available
                          unavailableSelect;      // select unavailable 10e+9 values or not

    private final int peakCount, actualpeakCount, aaCount, alpha, gamma, //beta,
                      constraintPeakCount,  // percent value for selection from the most constrained peaks
                      nextPeakSelect,       // number of the method for selection next peak
                      scoreConstraints[],   // number of 10e9 values per peak
                      totalScoreCalcCoef,   // a coofficient for total score calculation
                      localSearchStop,      // specifies when to stop local search. see "parameters.txt",
                      candSizeStart       // initial number of candSize (percent * aaCount / 100)
                      ;      // which value will be unavailable
                                             // (may be deleted if it always 10e+9)
    float HN_HN[][],HN_HA[][],HA_HN[][];
    public double totalScore,              // totalScore - current sum of combined scores,
                  totalScoreBeforeLS, accuracyBeforeLS, 
                  updateScore,             // updateScore - pheromone update score (setUpdateScore)
                  pheromone[][], heuristicBetaPower[][],unavailableValue;
    private final double infeasibleScore,   // infeasibleScore - score for 10e+9 assignments
                         combinedScore[][], localPheromoneUpdateValue;
                // list of peaks to which an aa can be assigned
	private  ArrayList<Integer> noe_HN_HN[], noe_HN_HA[],noe_HA_HN[];
	private  ArrayList<Float> UB_HN_HN[], UB_HN_HA[],UB_HA_HN[];
	private  ArrayList<Boolean> HN_HA_v[],HA_HN_v[];
	private final ArrayList<Integer> candidateList[];
	private final ArrayList<Integer> candidateListAA[];
	

    private long tProb = 0, tConstr = 0, tFind = 0, tNext = 0, tSort = 0;

    public void sayTime(String fileName) {
        try {
            FileWriter fr = new FileWriter(fileName, true);
            fr.write("Ant " + number + ": Constraints: " + tConstr + " Probabilities: "
                     + tProb + " Find: " + tFind + " NextPeakSelect: " + tNext + " Sort: " + tSort + "\n");
            fr.close();
        } catch (Exception e) {};
    }

    public Ant(int num, int actpeak, double[][] cmbnd,float[][] HN_HN,float [][] HN_HA,float [][] HA_HN,ArrayList<Integer>[] noe_HN_HN,ArrayList<Integer>[] noe_HN_HA,
    		ArrayList<Integer>[] noe_HA_HN, ArrayList<Float>[] UB_HN_HN, ArrayList<Float>[] UB_HN_HA, ArrayList<Float>[] UB_HA_HN, double infsblScr, int a, int g, //int b, was for beta
               double[][] phrmn, double[][] hrstc, boolean unavailSlct, double unavailVal, boolean infsblSel,
               double lclPhrmnUpdtVal, int cnstrntPkCnt, int nxtPkSlct, int searchStop, int ttlScrCoef,
               int candListSize, ArrayList<Integer>[] candidate, ArrayList<Integer>[] candidateAA,
               int[] scrConstr) {
        number = num;
        this.HN_HA = HN_HA;
        this.HA_HN = HA_HN;
        this.HN_HN = HN_HN;
        this.UB_HN_HN = UB_HN_HN;
        this.UB_HN_HA = UB_HN_HA;
        this.UB_HA_HN = UB_HA_HN;
        combinedScore = cmbnd;
        pheromone = phrmn;
        heuristicBetaPower = hrstc;
        this.noe_HN_HA = noe_HN_HA;
        this.noe_HA_HN = noe_HA_HN;
        this.noe_HN_HN = noe_HN_HN;
        peakCount = noe_HN_HA.length;
        actualpeakCount = actpeak;
        aaCount = combinedScore[0].length;
        infeasibleScore = infsblScr;
        alpha = a;
        gamma = g;
        candSizeStart = candListSize;
        infeasibleSelect = infsblSel;
        unavailableSelect = unavailSlct;
        unavailableValue = unavailVal;
        localPheromoneUpdateValue = lclPhrmnUpdtVal;
        constraintPeakCount = peakCount * cnstrntPkCnt / 100;
        nextPeakSelect = nxtPkSlct;
        localSearchStop = searchStop;
        totalScoreCalcCoef = ttlScrCoef;
        candidateList = candidate;
        candidateListAA = candidateAA;
        scoreConstraints = scrConstr;
    }

    public void resetAnt(int strt) {
        curPeak = startPeak = strt;
        stopWorking = false;
        totalScore = totalScoreBeforeLS = accuracyBeforeLS = 0;
        updateScore = 0;
        curStep = 0;
        candSize = candSizeStart;
        assignedAACount = 0;
        peakConstraints = new int[peakCount];
        peakAssignment = new Assignment[peakCount];
        peakMemory = new int[peakCount];
        peakMemoryReverse = new int[peakCount];
        availableAA = new int[aaCount];
        for (int i=0; i<aaCount; i++) availableAA[i] = 0;                   // ESKIDEN 1 OLUYORDU. SIMDI 0
        for (int i=0; i<peakCount; i++) {
//            peakConstraints[i] = -1;
        
            peakConstraints[i] = unavailableSelect ? getScoreConstraints(i, new int[candidateList[i].size()], candidateList[i]) // new int array is passed for no reason
                                                   : getScoreConstraints(i, new int[candidateList[i].size()], candidateList[i]) * 2 ;
            peakAssignment[i] = new Assignment(-1, false);
            peakMemory[i] = -1;
            peakMemoryReverse[i] = -1;
        }
        peakMemory[0] = curPeak;
        peakMemoryReverse[curPeak] = 0;
    }

    public void updatePheromone( double weight ) {
        if (stopWorking) return;
        for (int i=0; i<peakCount; i++)
             pheromone[i][peakAssignment[i].aminoAcid] += (updateScore * weight);
    }

    public void localUpdatePheromone() {
        pheromone[curPeak][peakAssignment[curPeak].aminoAcid] *= localPheromoneUpdateValue;
    }

    public int getCurrentPeak() { return curPeak; }

    public int getCurrentAssignment() { return peakAssignment[curPeak].aminoAcid; }

    // Will find an AA and assign it to current peak
    // When an ant stopped working it will wait until other ants finish current
    //      iteration and will start at the next iteration
    public void makeDecision() {
        long time1 = System.nanoTime();
        Assignment assignment = findAminoAcid();
        /*System.out.print("CurPeak ="+curPeak+"aminoAcid ="+assignment.aminoAcid+"    feasibility =");
        System.out.println(assignment.feasibility);
        for (int row=0; row<peakAssignment.length; row++)
            System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
        System.out.println();*/
        //System.out.println("findAminoAcid()   "+assignment.aminoAcid +"  "+ assignment.feasibility);
        tFind += System.nanoTime() - time1;
        if (assignment != null) {                                        // if something is found to assign
            peakAssignment[curPeak].aminoAcid = assignment.aminoAcid;
            peakAssignment[curPeak].feasibility = assignment.feasibility;
            // next contraint peak operations
            if (nextPeakSelect == 2) {
                int i, j;
                ArrayList<Integer> cand = candidateListAA[assignment.aminoAcid];
                peakConstraints[curPeak] = -1;
                for (i = 0; i < cand.size(); i++)
                    if (peakConstraints[cand.get(i)] != -1) {
                        peakConstraints[cand.get(i)] += 2;
                        if (combinedScore[cand.get(i)][assignment.aminoAcid] == unavailableValue)
                            peakConstraints[cand.get(i)] -= unavailableSelect ? 1 : 2;
                    }
                ArrayList<Integer> noe;
                int noeCount, noePeak;
 
                noe = noe_HN_HN[curPeak];
                noeCount = noe.size(); noePeak=-1;
                for (i = 0; i < noeCount; i++) {
                    noePeak = noe.get(i);
                    if (peakConstraints[noePeak] == -1) continue;
                    cand = candidateList[noePeak];
                    for (j = 0; j < cand.size(); j++) {
                        // bu kisim tartisilir kisimdir. 1, 2 olayindan dolayi. yani bir peak secildiyi zaman 1 mi 2 mi yoxsa her ikisi mi?
                        if (this.HN_HN[cand.get(j) ][assignment.aminoAcid ] > this.UB_HN_HN[curPeak].get(i))
                            peakConstraints[noePeak] += infeasibleSelect ? 1 : 2;
//                        if (combinedScore[noePeak][cand.get(j)] == unavailableValue )
//                            peakConstraints[noePeak] -= unavailableSelect ? 1 : 2;
                    }
                }
                
                noe = noe_HN_HA[curPeak];
                noeCount = noe.size(); noePeak=-1;
                for (i = 0; i < noeCount; i++) {
                    noePeak = noe.get(i);
                    if (peakConstraints[noePeak] == -1) continue;
                    cand = candidateList[noePeak];
                    for (j = 0; j < cand.size(); j++) {
                        // bu kisim tartisilir kisimdir. 1, 2 olayindan dolayi. yani bir peak secildiyi zaman 1 mi 2 mi yoxsa her ikisi mi?
                        if (this.HA_HN[cand.get(j) ][assignment.aminoAcid ] >this.UB_HN_HA[curPeak].get(i))
                            peakConstraints[noePeak] += infeasibleSelect ? 1 : 2;
//                        if (combinedScore[noePeak][cand.get(j)] == unavailableValue )
//                            peakConstraints[noePeak] -= unavailableSelect ? 1 : 2;
                    }
                }
                
                
                noe = noe_HA_HN[curPeak];
                noeCount = noe.size(); noePeak=-1;
                for (i = 0; i < noeCount; i++) {
                    noePeak = noe.get(i);
                    if (peakConstraints[noePeak] == -1) continue;
                    cand = candidateList[noePeak];
                    for (j = 0; j < cand.size(); j++) {
                        // bu kisim tartisilir kisimdir. 1, 2 olayindan dolayi. yani bir peak secildiyi zaman 1 mi 2 mi yoxsa her ikisi mi?
                        if (this.HN_HA[cand.get(j)  ][assignment.aminoAcid] >this.UB_HA_HN[curPeak].get(i))
                            peakConstraints[noePeak] += infeasibleSelect ? 1 : 2;
//                        if (combinedScore[noePeak][cand.get(j)] == unavailableValue )
//                            peakConstraints[noePeak] -= unavailableSelect ? 1 : 2;
                    }
                }
            }

            if (candSize < aaCount) candSize++;             // adds one to cand to extend the candidate list
            assignedAACount++;
            if (assignment.aminoAcid != aaCount-1)  //seyma  dummyAA değilse available kısmını -1 yap
                availableAA[assignment.aminoAcid] = -1;
            peakMemory[curStep] = curPeak;
            peakMemoryReverse[curPeak] = curStep;
        } else                               // if nothing is found
            stopWorking = true;
    }

    public void selectNextPeak() {
    	
        curStep++;
        //System.out.println("selectNextPeak is calleddddddddddddddddddddddddddddddddddddddd"+" curStep="+curStep);
        if ( !completed() )                 // if it hasn't finished current circle
            switch (nextPeakSelect) {
                case 0: { curPeak = nextSequentialPeak(); break; }
                case 1: { curPeak = nextRandomPeak();     break; }
                case 2: { curPeak = nextConstraintPeak(); break; }
            }
        else {                               // if it finished it's current circle
            totalScore = calculateDeltaScore(peakAssignment);
            setUpdateScore();
            //constructNoeViolation(peakAssignment);
            
            //System.out.println("selectNextPeak is called with CurPeak======AminoAcid Numberrrr"+" curStep="+curStep);
        }
    }

    private Assignment findAminoAcid() {

        ArrayList<Integer> candidate = candidateList[curPeak];
        //System.out.println("candidateList "+candidate);
        int i, highIndex = -1, length = candSize,                 // bunu (length'i) 10e+9 sonra hepsini kapsayacak sekilde yap.
        		avail[] = new int[length];
        double values[] = new double[length],
               sum = 0, sum1 = 0, randValue;
        long time1 = System.nanoTime();
        for (i=0; i<length; i++) avail[i] = availableAA[candidate.get(i)];
        getScoreConstraints(curPeak, avail, candidate);
        if (assignedAACount > 0) getNoeConstraints(curPeak, avail, candidate);
        tConstr += System.nanoTime() - time1;

        // values may be determined in the ant class as an attribute
        time1 = System.nanoTime();
        for (i=0; i<length; i++) {                                // initialize values
            if ( avail[i] == -1 )
                values[i] = 0;
            else if ( avail[i] == 0 )
                values[i] = Math.pow(pheromone[curPeak][candidate.get(i)], alpha) * heuristicBetaPower[curPeak][candidate.get(i)];
            else
                values[i] = Math.pow(pheromone[curPeak][candidate.get(i)], alpha) * heuristicBetaPower[curPeak][candidate.get(i)] / Math.pow( avail[i] * infeasibleScore, gamma );

            if (Double.isInfinite(values[i])) values[i] = Double.MAX_VALUE;
            sum += values[i];
        }
        if (sum == 0) {                             // nothing to assign. all avail[i]'s are = -1 that is sum = 0
            tProb += System.nanoTime() - time1;     // can't be if feasibleSelect and unavailableSelect are TRUE
            return null;
        }

        if (number==1)
            number = 1;

        randValue = new Random().nextDouble();                      // selecting among values
        for (i=0; i<length; i++) {                                  // normalize values
            sum1 += values[i] /= sum;
            if ( sum1 > randValue ) { highIndex = i; break; }
        }
        tProb += System.nanoTime() - time1;

        if ( avail[highIndex] == 0 )          // seperate infeasible (false) and feasible (true) assignments with boolean value
            return new Assignment(candidate.get(highIndex), true);
        else
            return new Assignment(candidate.get(highIndex), false);
    }

    // for sequential peak assignment e.g. IF startPeak=5 THEN nextPeak=6 THEN nextPeak=7
    private int nextSequentialPeak() { return (curPeak + 1) % peakCount; }

    // random selection of next peak.
    // this code can be optimised !!!!!!!
    private int nextRandomPeak() {
        ArrayList<Integer> availablePeaks = new ArrayList<Integer>();
        for (int i=0; i<peakCount; i++)
            if (peakAssignment[i].aminoAcid == -1) availablePeaks.add(i);
        return availablePeaks.get( new Random().nextInt(availablePeaks.size()) );
    }

    // selection of the next peak is biased toward contrainted peaks (which has less AA's to assign )
    private int nextConstraintPeak() {
        int mostConstraintPeaks[] = new int[peakCount],
            constraints[] = new int[peakCount], i;

        for (i = 0; i < peakCount; i++) {
            mostConstraintPeaks[i] = i;
            constraints[i] = peakConstraints[i];
        }
        long time = System.nanoTime();
        quicksort(constraints, mostConstraintPeaks, 0, mostConstraintPeaks.length-1);
        tSort += System.nanoTime() - time;

        int randBound = constraintPeakCount;
        for (i=0; i<constraintPeakCount; i++)
            if (constraints[i]==-1) {
                randBound = i;
                break;
            }
        return mostConstraintPeaks[new Random().nextInt(randBound)];
    }

    public void setUpdateScore() { updateScore = 1 / Math.pow(totalScore, 1.0/totalScoreCalcCoef); }

    // in this procedure contraint is 10e9 value, so this procedure try to prevent 10e9 from being selected
    private int getScoreConstraints(int peak, int[] avail, ArrayList<Integer> cand) {
        int res = 0;
        for (int i = 0; i < avail.length; i++)
            if ( (combinedScore[peak][cand.get(i)] == unavailableValue) && (avail[i]!=-1) ) {
                if (unavailableSelect)
                    avail[i]++;
                else
                    avail[i] = -1;
                res++;
            }
        return res;
    }

    private void getNoeConstraints(int peak, int[] avail, ArrayList<Integer> cand) {
        int i, j, noeCount, noePeak, noeAA;
        ArrayList<Integer> noe;
        
        noe = noe_HN_HN[peak];
        noeCount = noe.size();
        for (i = 0; i < noeCount; i++) {
            noePeak = noe.get(i);
            noeAA = peakAssignment[noePeak].aminoAcid;
            if (noeAA != -1)
                for (j = 0; j < avail.length; j++)
                    if ( (this.HN_HN[cand.get(j)][noeAA]>this.UB_HN_HN[peak].get(i)) && (avail[j]!=-1) )
                        if (infeasibleSelect)
                            avail[j]++;
                        else
                            avail[j] = -1;
        }
        
        noe = noe_HN_HA[peak];
        noeCount = noe.size();
        for (i = 0; i < noeCount; i++) {
            noePeak = noe.get(i);
            noeAA = peakAssignment[noePeak].aminoAcid;
            if (noeAA != -1)
                for (j = 0; j < avail.length; j++)
                    if ( (this.HN_HA[cand.get(j)][noeAA]>this.UB_HN_HA[peak].get(i)) && (avail[j]!=-1) )
                        if (infeasibleSelect)
                            avail[j]++;
                        else
                            avail[j] = -1;
        }
        
        
        noe = noe_HA_HN[peak];
        noeCount = noe.size();
        for (i = 0; i < noeCount; i++) {
           noePeak = noe.get(i);
           noeAA = peakAssignment[noePeak].aminoAcid;
           if (noeAA != -1)
              for (j = 0; j < avail.length; j++)
               if ( (this.HA_HN[cand.get(j)][noeAA]>this.UB_HA_HN[peak].get(i)) && (avail[j]!=-1) )
                if (infeasibleSelect)
                    avail[j]++;
               else
                   avail[j] = -1;
          }
    }

    public void printAssignResults() {
//        if (stopWorking) {
//            System.out.println("Ant "+number+" didn't find any complete assignments.");
//            return;
//        }
//        System.out.println("Printing assign results for ant " + number);
        for (int i=0; i<peakAssignment.length; i++)
            System.out.print("<"+i+"-"+peakAssignment[i].aminoAcid+", "+peakAssignment[i].feasibility+"> ");
//        int i;
//        for (i=0; i<peakMemory.length; i++)
//            if (peakMemory[i] != -1)
//                System.out.print("<"+peakMemory[i]+"-"+peakAssignment[peakMemory[i]].aminoAcid+", "+peakAssignment[peakMemory[i]].feasibility+"> ");
//            else break;
//        System.out.println("\n" + curStep);
        System.out.println("\nTotal score of the assignment = " + new DecimalFormat("0.0000").format(totalScore) );
        System.out.println("Accuracy = " + accuracy());
        System.out.println("No. of infeasible assignments: " + getInfeasibleCount());
        System.out.println();
    }

    public double getTotalScore() { 
        if ( !stopWorking ) return (double) Math.round(totalScore * 1000000) / 1000000;
        else return -1;
    }

    private boolean completed() { return curStep == peakCount; }

    private double setTotalScore( Assignment[] completeAssignment ) {
        double res = 0;
        for (int i = 0; i < peakCount; i++) {
            if ( ! completeAssignment[i].feasibility )                  // bu duruma gore degisiyor. BUNA 31/03/2011 TARIHINDE ANLASTIK
                res += infeasibleScore;
            else
                res += combinedScore[i][completeAssignment[i].aminoAcid];
//            res += combinedScore[i][completeAssignment[i].aminoAcid];  // eskiden yaptigimdir
//            if ( ! completeAssignment[i].feasibility )
//                res += infeasibleScore;
        }
        return res;
    }

    public void localSearchTwoOpt() {
        // consider LS stop conditions. becuase you changed this code. and now it works only with "stop when no improvement"

        /*for (int row=0; row<peakAssignment.length; row++)
            System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
        System.out.println();
    	System.out.println("Total Score="+calculateDeltaScore(peakAssignment));
    	double score=calculateDeltaScore(peakAssignment);*/
        totalScoreBeforeLS = totalScore;
        accuracyBeforeLS = accuracy();
        if ( !completed() || stopWorking ) return;

        ArrayList<Integer> swapList = new ArrayList<Integer>(), noe;
        int i, j, k, aa1, aa2, maxI, maxJ, pCount;
//        Assignment assignment[] = new Assignment[peakCount];
        double maxDelta, delta[][]= new double[peakCount][peakCount], tempScore;
        for (i=0; i<peakCount; i++) {                            // create new empty assignment
//            assignment[i] = new Assignment(-1, false);
            swapList.add(i);
        }

//        System.out.println("\n" + number);
        int counter=0;
        while (true) {
//            for (k = 0; k < peakCount; k++) {                   // take current assignment
//                assignment[k].aminoAcid = peakAssignment[k].aminoAcid;
//                assignment[k].feasibility = peakAssignment[k].feasibility;
//            }

            pCount = swapList.size();
            for (i=0; i<pCount; i++) {                 // swapLIst.size() = count;
                aa1 = peakAssignment[swapList.get(i)].aminoAcid;
                for (j = 0; j < peakCount; j++) {
                    if (j == swapList.get(i)) {         // don't swap the same peaks :)
                        delta[j][j] = Double.POSITIVE_INFINITY;
                        continue;
                    }
                    aa2 = peakAssignment[j].aminoAcid;
                    if (!unavailableSelect)
                        if ((combinedScore[swapList.get(i)][aa2] == unavailableValue) ||
                            (combinedScore[j][aa1] == unavailableValue)) {
                            delta[swapList.get(i)][j] = Double.POSITIVE_INFINITY;
                            continue;
                        }

                    peakAssignment[swapList.get(i)].aminoAcid = aa2;
                    peakAssignment[j].aminoAcid = aa1;
                    
                    /*tempScore = 0;
                    tempScore -= peakAssignment[swapList.get(i)].feasibility ? combinedScore[swapList.get(i)][aa1] : infeasibleScore;
                    tempScore -= peakAssignment[j].feasibility ? combinedScore[j][aa2] : infeasibleScore;
                    //System.out.println("peak1 ="+swapList.get(i)+"   peak2="+j);*/
                    delta[swapList.get(i)][j] = deltaScore(peakAssignment, swapList.get(i), j);
                    delta[j][swapList.get(i)] = delta[swapList.get(i)][j];

                    peakAssignment[swapList.get(i)].aminoAcid = aa1;
                    peakAssignment[j].aminoAcid = aa2;
                    
                    
                    //calculateDeltaScore(peakAssignment);
                }
            }

            maxDelta = delta[0][0]; maxI = 0; maxJ = 0;
            for (i=0; i<peakCount; i++)
            for (j=0; j<peakCount; j++)
                if (delta[i][j] < maxDelta) {
                    maxDelta = delta[i][j];
                    maxI = i;
                    maxJ = j;
                }


            // if we gained imporvment
            if (maxDelta < -0.0001) {

                totalScore += maxDelta;
                
                aa1 = peakAssignment[maxI].aminoAcid;
                peakAssignment[maxI].aminoAcid = peakAssignment[maxJ].aminoAcid;
                peakAssignment[maxJ].aminoAcid = aa1;
                //UpdateNoeViolation(peakAssignment,maxI, maxJ,HN_HA_v, HA_HN_v);
                //System.out.println(maxI+"<->"+maxJ+": " +maxDelta + ";    ");

                calculateDeltaScore(peakAssignment);
                swapList = new ArrayList<Integer>();         // update swap list
                swapList.add(maxI);
                swapList.add(maxJ);
                
                noe = noe_HN_HN[maxI];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                noe = noe_HN_HN[maxJ];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                k = swapList.size();
                for (j=2; j<k; j++) {
                    noe = noe_HN_HN[swapList.get(j)];
                    pCount = noe.size();
                    for (i=0; i<pCount; i++)
                        if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                }
                
                noe = noe_HN_HA[maxI];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                noe = noe_HN_HA[maxJ];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                k = swapList.size();
                for (j=2; j<k; j++) {
                    noe = noe_HN_HA[swapList.get(j)];
                    pCount = noe.size();
                    for (i=0; i<pCount; i++)
                        if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                }
                   
                noe = noe_HA_HN[maxI];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                noe = noe_HA_HN[maxJ];
                pCount = noe.size();
                for (i=0; i<pCount; i++)
                    if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                k = swapList.size();
                for (j=2; j<k; j++) {
                    noe = noe_HA_HN[swapList.get(j)];
                    pCount = noe.size();
                    for (i=0; i<pCount; i++)
                        if (swapList.indexOf(noe.get(i)) == -1) swapList.add(noe.get(i));
                }              
                
            } else break;
        }
        setUpdateScore();
    }
    
    // works only for the last total score calculation agreement. (total += feas ? score : penalty; )
    private double deltaScore( Assignment[] assignment, int peak1, int peak2) {
        double res=0;
        /*Scanner input = new Scanner(System.in);
        for (int row=0; row<peakAssignment.length; row++)
            System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
        System.out.println();*/
        
        
        ArrayList<Integer> peakList=new ArrayList <Integer> (), noe=new ArrayList <Integer> ();
        boolean feas1 = true,feas2=true;
        int peak,aa = -1,noePeak=-1,a1=-1,a2=-1;
        
        a1=assignment[peak1].aminoAcid;
        a2=assignment[peak2].aminoAcid;
        //System.out.println("Swapp  peaks "+peak1+" <-> "+peak2 +"; AA "+a1+" <-> "+a2);
        //System.out.println("peak1 ->"+peak1);
        
  	    noe=noe_HN_HN[peak1];
  	    //System.out.println("noe HN_HA ->"+noe);
  	    for(int i=0; i<noe.size();i++){
  		  noePeak=noe.get(i);
  		  //System.out.print(HN_HA[a1][assignment[noePeak].aminoAcid] +"  ");
  		  if(HN_HN[a1][assignment[noePeak].aminoAcid]>UB_HN_HN[peak1].get(i)){
  			  feas1=false;
  		  }//else
  		  
  			  if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
  					  && noePeak!=peak2 && noePeak!=peak1){
  				  peakList.add(noePeak);
  			  }
  		   
  	    }
        
  	    noe=noe_HN_HA[peak1];
  	    //System.out.println("noe HN_HA ->"+noe);
  	    for(int i=0; i<noe.size();i++){
  		  noePeak=noe.get(i);
  		  //System.out.print(HN_HA[a1][assignment[noePeak].aminoAcid] +"  ");
  		  if(HN_HA[a1][assignment[noePeak].aminoAcid]>UB_HN_HA[peak1].get(i)){
  			  feas1=false;
  		  }//else
  		  
  			  if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
  					  && noePeak!=peak2 && noePeak!=peak1){
  				  if(peakList.indexOf(noePeak)==-1) peakList.add(noePeak);
  			  }
  		   
  	    }
  	  //input.next();
  	  //System.out.println();
  	   noe=noe_HA_HN[peak1];
  	   //System.out.println("noe HA_HN ->"+noe);
  	   for(int i=0; i<noe.size(); i++){
  		  noePeak=noe.get(i);
  		//System.out.print(HA_HN[a1][assignment[noePeak].aminoAcid]+"  ");
  		  if(HA_HN[a1][assignment[noePeak].aminoAcid]>UB_HA_HN[peak1].get(i)){
  			  feas1=false;
  		   }//else
  		   
  			   if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
  					  && noePeak!=peak2 && noePeak!=peak1){
  				  if(peakList.indexOf(noePeak)==-1) peakList.add(noePeak);
  					  
  			   }
  		   
  	    }
  	 //System.out.println();
  	 //System.out.println("feas1 ="+feas1);
  	//System.out.println("peak list "+peakList);
  	  
  	//input.next();
      
        if(assignment[peak1].feasibility){
        	     if(combinedScore[peak1][a1]!=unavailableValue && feas1){
        	        		  res=res -combinedScore[peak1][a2]+combinedScore[peak1][a1];
        	        		  //System.out.println("Feasibility of peak1= "+peak1+" was true, now also its true; res="+res);
        	       }else{
        	        		  res=res+infeasibleScore-combinedScore[peak1][a2];
        	        		  //System.out.println("Feasibility of peak1= "+peak1+" was true, now its false; res="+res);
        	       } 	                 	
        }else{
        	if(combinedScore[peak1][a1]!=unavailableValue && feas1){
        		res=res-infeasibleScore+combinedScore[peak1][a1];
        		//System.out.println("Feasibility of peak1= "+peak1+" was false, now also its true; res="+res);
        	}
        }

       //System.out.println("res after peak1="+res);
       //input.next();
        
       //System.out.println("peak2 ->"+peak2+"a2 ="+a2);
        
    	feas1=true; feas2=true;
    	
    	noe=noe_HN_HN[peak2];
    	//System.out.println("noe_HN_HA ->"+noe);
    	for(int i=0; i<noe.size(); i++){
    		noePeak=noe.get(i);
    		//System.out.print(HN_HA[a2][assignment[noePeak].aminoAcid]+"  ");
    		if(HN_HN[a2][assignment[noePeak].aminoAcid]>UB_HN_HN[peak2].get(i)){
    			feas1=false;
    		}//else
    		
    			if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
    					&& noePeak!=peak1 && noePeak!=peak2){
    				if(peakList.indexOf(noePeak)==-1) peakList.add(noePeak);
    			}
    		
    	}
    	
    	noe=noe_HN_HA[peak2];
    	//System.out.println("noe_HN_HA ->"+noe);
    	for(int i=0; i<noe.size(); i++){
    		noePeak=noe.get(i);
    		//System.out.print(HN_HA[a2][assignment[noePeak].aminoAcid]+"  ");
    		if(HN_HA[a2][assignment[noePeak].aminoAcid]>UB_HN_HA[peak2].get(i)){
    			feas1=false;
    		}//else
    		
    			if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
    					&& noePeak!=peak1 && noePeak!=peak2){
    				if(peakList.indexOf(noePeak)==-1) peakList.add(noePeak);
    			}
    		
    	}
    	//input.next();
    	//System.out.println();
    	noe=noe_HA_HN[peak2];
    	//System.out.println("noe_HA_HN ->"+noe);
    	for(int i=0; i<noe.size(); i++){
    		noePeak=noe.get(i);
    		//System.out.print(HA_HN[a2][assignment[noePeak].aminoAcid]+"  ");
    		if(HA_HN[a2][assignment[noePeak].aminoAcid]>UB_HA_HN[peak2].get(i)){
    			feas1=false;
    		}//else
    		
    			if(combinedScore[noePeak][assignment[noePeak].aminoAcid]!=unavailableValue
    					&& noePeak!= peak1 && noePeak!= peak2){
    				if(peakList.indexOf(noePeak)==-1) peakList.add(noePeak);
    			}
    		
    	}
    	//System.out.println();
        //System.out.println("feas1 ="+feas1);
        //System.out.println("peak list "+peakList);
    	
        //input.next();
    	
        if(assignment[peak2].feasibility){       	
        	if(combinedScore[peak2][a2]!=unavailableValue && feas1){
        		res=res-combinedScore[peak2][a1]+combinedScore[peak2][a2];
        		//System.out.println("Feasibility of peak2= "+peak2+" was true, now also its true; res= "+res);
        	}else{
        		res=res+infeasibleScore-combinedScore[peak2][a1];
        		//System.out.println("Feasibility of peak2= "+peak2+" was true, but now  its false; res="+res);
        	}
        	
        }else{
        	if(combinedScore[peak2][a2]!=unavailableValue && feas1){
        		res=res-infeasibleScore+combinedScore[peak2][a2];
        		//System.out.println("Feasibility of peak2= "+peak2+" was false, now  its true; res="+res);
        	}
        }
        
        //input.next();
        
        //System.out.println("peak list "+peakList);
        for(int i=0; i<peakList.size();i++){
  		  
            peak=peakList.get(i);
            aa=assignment[peak].aminoAcid;
            feas2=true;
            //System.out.println("peak= "+peak+";  aa="+aa);
            
            
                 noe=noe_HN_HN[peak];
                 for(int j=0;j<noe.size();j++){
                    noePeak=noe.get(j);
                   //System.out.print(HN_HA[aa][assignment[noePeak].aminoAcid]+"  ");
	               if(HN_HN[aa][assignment[noePeak].aminoAcid]>UB_HN_HN[peak].get(j)){
		            feas2=false;
            	    }
                   }         
                 noe=noe_HN_HA[peak];
	              for(int j=0;j<noe.size();j++){
	              noePeak=noe.get(j);
	              //System.out.print(HN_HA[aa][assignment[noePeak].aminoAcid]+"  ");
		           if(HN_HA[aa][assignment[noePeak].aminoAcid]>UB_HN_HA[peak].get(j)){
			         feas2=false;
	             	  }
	               }
	             noe=noe_HA_HN[peak];
	             for(int j=0; j<noe.size(); j++){
	             noePeak=noe.get(j);
	             //System.out.print(HA_HN[aa][assignment[noePeak].aminoAcid]+"  ");
		          if(HA_HN[aa][assignment[noePeak].aminoAcid]>UB_HA_HN[peak].get(j)){
			       feas2=false;
      		  }
        	      }
	     //System.out.println();
	     //System.out.println("feas peak ="+assignment[peak].feasibility+";  feas now="+feas2);
	     //input.next();
	     
         if(assignment[peak].feasibility){ 
            if(!feas2){
               res=res+infeasibleScore-combinedScore[peak][aa];
               //System.out.println("Feasibility of peak= "+peak+" was true,but now its false; res="+res);
             }
          }else{
            if(feas2){
              res=res-infeasibleScore+combinedScore[peak][aa];
              //System.out.println("Feasibility of peak= "+peak+" was false,but now its true; res="+res);
          }
        }
       }
        
        /*System.out.println("res after peak2="+res);
        System.out.println("score+res="+(res+score));
        System.out.println("cost="+calculateDeltaScore(assignment));
        if(res+score!=calculateDeltaScore(assignment)) 
        	System.out.println("\n\n\n\n\n Probleeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeemmmmmmmmmmmmmmmmmmmmmmmmmmmmm\n\n\n\n\n");
        
        for (int row=0; row<peakAssignment.length; row++)
            System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
        System.out.println();*/
       
        return res;
    
     }

   
   /*
   private void UpdateNoeViolation(Assignment [] assignment, int peak1,int peak2, ArrayList<Boolean> HN_HA_vio[],ArrayList<Boolean> HA_HN_vio[]){
	   int aa1= assignment[peak1].aminoAcid;
	   int aa2= assignment[peak2].aminoAcid;
	   int noePeak;
	   
	   for(int i=0; i<HN_HA_vio[peak1].size();i++){
		   if(HN_HA[aa1][noe_HN_HA[peak1].get(i)]==1){
			   HN_HA_vio[peak1].set(i,true);
		   }else{
			   HN_HA_vio[peak1].set(i,false);
		   }
	   }
	   
	   for(int i=0; i<HA_HN_vio[peak1].size();i++){
		   if(HA_HN[aa1][noe_HA_HN[peak1].get(i)]==1){
			   HA_HN_vio[peak1].set(i,true);
		   }else{
			   HA_HN_vio[peak1].set(i,false);
		   }
	   }
	   //--------------------------------------------------
	   
	   for(int i=0; i<HN_HA_vio[peak2].size();i++){
		   if(HN_HA[aa2][noe_HN_HA[peak2].get(i)]==1){
			   HN_HA_vio[peak2].set(i,true);
		   }else{
			   HN_HA_vio[peak2].set(i,false);
		   }
	   }
	   
	   for(int i=0; i<HA_HN_v[peak2].size();i++){
		   if(HA_HN[aa2][noe_HA_HN[peak2].get(i)]==1){
			   HA_HN_vio[peak2].set(i,true);
		   }else{
			   HA_HN_vio[peak2].set(i,false);
		   }
	   }
   }
   
   private void constructNoeViolation(Assignment [] assignment){
	   this.HN_HA_v = new ArrayList[peakCount];
	   this.HA_HN_v = new ArrayList[peakCount];
	   for (int i=0; i<peakCount; i++){
		   this.HN_HA_v[i] = new ArrayList<Boolean>();
		   this.HA_HN_v[i] = new ArrayList<Boolean>();}
	   int noePeak,aa;
	   for (int i=0; i<peakCount; i++){
		   aa=assignment[i].aminoAcid;
		   for(int j=0; j<this.noe_HN_HA[i].size(); j++){
			   if(HN_HA[aa][this.noe_HN_HA[i].get(j)]==1){
				   HN_HA_v[i].add(true);
			   }else{HN_HA_v[i].add(false);}
		   }
		   
		   for(int j=0; j<this.noe_HA_HN[i].size(); j++){
			   if(HA_HN[aa][this.noe_HA_HN[i].get(j)]==1){
				   HA_HN_v[i].add(true);
			   }else{HA_HN_v[i].add(false);}
		   }
		   
	   }
	   

	   
   }*/

    private double calculateDeltaScore( Assignment[] assignment ) {
    	//System.out.println("calculateDeltaScore is called");

        int aa, i, j, l, peakCur;
        ArrayList<Integer> noe;

        for (i=0; i<peakCount; i++) {
            aa = assignment[i].aminoAcid;
            if ( combinedScore[i][aa] == unavailableValue ) {
                assignment[i].feasibility = false;              // if 10E+9 is selected then make it feasiblity false
                continue;                                       // NOTE: if we proceeded to this point then we have unavilableSelect = true
            }
             assignment[i].feasibility = true;
            
             noe = noe_HN_HN[i];                                   // look for noe violation
             l = noe.size();
             for (j=0; j<l; j++) {
                 peakCur = noe.get(j);
                 if (this.HN_HN[aa][assignment[peakCur].aminoAcid ] > this.UB_HN_HN[i].get(j)) {
                     assignment[i].feasibility = false;
                     //assignment[peakCur].feasibility = false;
                     if ( !infeasibleSelect ) return Double.NEGATIVE_INFINITY;
                     break;
                 }
             }
            
            noe = noe_HN_HA[i];                                   // look for noe violation
            l = noe.size();
            for (j=0; j<l; j++) {
                peakCur = noe.get(j);
                if (this.HN_HA[aa][assignment[peakCur].aminoAcid ] > this.UB_HN_HA[i].get(j)) {
                    assignment[i].feasibility = false;
                    //assignment[peakCur].feasibility = false;
                    if ( !infeasibleSelect ) return Double.NEGATIVE_INFINITY;
                    break;
                }
            }
            
            
            noe = noe_HA_HN[i];                                   // look for noe violation
            l = noe.size();
            for (j=0; j<l; j++) {
                peakCur = noe.get(j);
                if (this.HA_HN[aa][assignment[peakCur].aminoAcid ] > this.UB_HA_HN[i].get(j)) {
                    assignment[i].feasibility = false;
                    //assignment[peakCur].feasibility = false;
                    if ( !infeasibleSelect ) return Double.NEGATIVE_INFINITY;
                    break;
                }
            }
            
        }
        
    	/*for (int row=0; row<peakAssignment.length; row++)
            System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
        System.out.println();*/
        return setTotalScore(assignment);
    }

    public Assignment[] getCompleteAssignment() {
        if ( !completed() || stopWorking ) return null;

        Assignment res[] = new Assignment[peakCount];
        for (int i=0; i<peakCount; i++) {
            res[i] = new Assignment(peakAssignment[i].aminoAcid, peakAssignment[i].feasibility);
        }
        return res;
    }

    public int getInfeasibleCount() {
        if ( !completed() || stopWorking ) return -1;

        int res = 0;
        for (int i=0; i<peakCount; i++)
            if ( peakAssignment[i].feasibility == false ) res++;
        return res;
    }

    // not yet optimised for newer methods. also may run incorrectly
 
    public double accuracy() {
        if ( !completed() || stopWorking ) return -1;
        
        double res = 0;
        for (int i=0; i<actualpeakCount; i++)
            if ( peakAssignment[i].aminoAcid == i ) res++;
        for (int i=actualpeakCount; i<peakCount; i++)
            if ( peakAssignment[i].aminoAcid == aaCount-1 ) res++;        
        return (double) Math.round( res / peakCount * 10000 ) / 100;
    }

    public void quicksort(int[] array, int[] index, int left, int right) {
        int pivot, leftIdx = left, rightIdx = right, k;
        if (right - left > 0) {
            pivot = (left + right) / 2;
            while (leftIdx <= pivot && rightIdx >= pivot) {
                while (array[leftIdx] > array[pivot] && leftIdx <= pivot) leftIdx++;
                while (array[rightIdx] < array[pivot] && rightIdx >= pivot) rightIdx--;
                k = array[leftIdx]; array[leftIdx] = array[rightIdx];   array[rightIdx] = k;
                k = index[leftIdx]; index[leftIdx] = index[rightIdx];   index[rightIdx] = k;
                leftIdx++;  rightIdx--;
                if (leftIdx - 1 == pivot) pivot = ++rightIdx;
                else if (rightIdx + 1 == pivot) pivot = --leftIdx;
            }
            quicksort(array, index, left, pivot - 1);
            quicksort(array, index, pivot + 1, right);
        }
    }

	/*System.out.println("infeasibleScore ::: "+infeasibleScore);
	System.out.println("unavailableValue ::: "+unavailableValue);
    for (int row=0; row<peakAssignment.length; row++)
        System.out.print("<"+row+"-"+peakAssignment[row].aminoAcid+", "+peakAssignment[row].feasibility+"> ");
    System.out.println();
	System.out.println("totalScore == "+totalScore);
	System.out.println(maxDelta);*/

	/*int val;
	val= -(int) (combinedScore[maxI][peakAssignment[maxI].aminoAcid]==unavailableValue? infeasibleScore:combinedScore[maxI][peakAssignment[maxI].aminoAcid]);
	val= val- (int) (combinedScore[maxJ][peakAssignment[maxJ].aminoAcid]==unavailableValue? infeasibleScore:combinedScore[maxJ][peakAssignment[maxJ].aminoAcid]);

    System.out.print(maxI+"<->"+maxJ+": " +val
    		+"  "+maxDelta + ";    ");  System.out.print(peakAssignment[maxI].feasibility+"  "+peakAssignment[maxJ].feasibility+"   ");*/



  
}