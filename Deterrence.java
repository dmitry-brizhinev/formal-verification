//import java.util.Random;

    /*@ predicate states_initialised{L}(Deterrence [] states) = 
      @   states.length == Deterrence.numStates && !\fresh(states);
      @*/

    /*@ predicate state_valid{L}(Deterrence [] states, Deterrence state, int index) = 
      @   !\fresh(state) &&
          states[state.index] == state &&
          state.index == index &&
          !\fresh(state.numIgnores) && state.numIgnores.length    == Deterrence.ATTACK_TYPES && (\forall int j; j >= 0 && j < Deterrence.ATTACK_TYPES ==> state.numIgnores[j]    >= 1) &&
          !\fresh(state.numRetaliates) && state.numRetaliates.length == Deterrence.ATTACK_TYPES && (\forall int j; j >= 0 && j < Deterrence.ATTACK_TYPES ==> state.numRetaliates[j] >= 1) &&
          !\fresh(state.retaliationTable) && state.retaliationTable.length == Deterrence.ATTACK_TYPES && (\forall int j; j >= 0 && j < Deterrence.ATTACK_TYPES ==> 0.0 <= state.retaliationTable[j] < 1.0);
      @*/

    /*@ predicate states_valid_up_to{L}(Deterrence [] states, integer k) = 
      @   (\forall int i; i >= 0 && i < k ==> states[i].index == i) &&
          (\forall int i; i >= 0 && i < k ==> state_valid(states, states[i], i));
      @*/ 

    /*@ predicate states_valid{L}(Deterrence [] states) = 
      @   states.length == Deterrence.numStates && !\fresh(states) &&
      @   (\forall int i, int j; i >= 0 && i < Deterrence.numStates && j >= 0 && j < Deterrence.numStates && i != j ==> states[i] != states[j]) &&
      @   (\forall int i; i >= 0 && i < Deterrence.numStates ==> states[i].index == i) &&
          (\forall int i; i >= 0 && i < Deterrence.numStates ==> state_valid(states, states[i], i));
      @*/ 

    /*@ logic integer attackStrength_logic(integer intensity) = intensity + 1;
      @*/

    /*@ logic real expectedValue_logic(integer intensity, integer numIgnores, integer numRetaliates, real ATTRIBUTION_PROBABILITY, real RETALIATION_EFFECT) =
		attackStrength_logic(intensity)
        * (numIgnores + numRetaliates * ((1.0 - ATTRIBUTION_PROBABILITY) - RETALIATION_EFFECT * ATTRIBUTION_PROBABILITY))
        / (numIgnores + numRetaliates);
	  @*/

//@+ CheckArithOverflow=no 
public class Deterrence {

  //private static final Random r = new Random(54321);

  /*@ assigns \nothing;
      ensures \result >= 0.0 && \result < 1.0;
    @*/
  private static double nextDouble() {
    return 1.0/10.0;
    //return r.nextDouble();
  }

  /*@ requires a > 0;
      assigns \nothing;
    @ ensures \result >= 0 && \result < a;
    @*/
  private static int nextInt(int a) {
    return a - 1;
    //return r.nextInt(a);
  }

  private static final int numStates = 100;
  private static final int turnLength = 100;
  private static final int numTurns = 10000;
  private static final double mutateProbability = 0.05;

  private static Deterrence[] states;
  
  /*@ assigns \nothing;
      ensures \result == val || \result == mutated;
    @*/
  private static double mutate(double val, double mutated) {
      if (nextDouble() < mutateProbability)
          return mutated;
      else
          return val;
  }

  /*@ assigns \nothing;
      ensures \result == a || \result == b;        
    @*/
  private static double choice(double a, double b) {
      if (nextDouble() < 0.5)
          return a;
      else
          return b;
  }
    
  /*@ requires states_valid(states);
      assigns \nothing;
      ensures (\forall int i; i >= 0 && i < numStates ==> states[i].value >= \result.value) &&
    @         (\exists int i; i >= 0 && i < numStates && states[i] == \result);
    @*/
  private static Deterrence getWorst() {
      Deterrence worst = states[0];
      /*@
          loop_invariant (\forall int j; j >= 0 && j < h ==> states[j].value >= worst.value) &&
                         (\exists int k; k >= 0 && k < h &&  states[k] == worst) &&
                         h >= 0 && h <= numStates;
          loop_variant numStates - h;
        @*/
      for (int h = 1; h < numStates; h++) {
          if (states[h].value < worst.value) {
              worst = states[h];
          }
      }
      return worst;
  }
        
  /*@ requires states_valid(states);
      assigns \nothing;
      ensures (\forall int i; i >= 0 && i < numStates ==> states[i].value <= \result.value) &&
              (\exists int i; i >= 0 && i < numStates && states[i] == \result);
    @*/
  private static Deterrence getBest() {
      Deterrence best = states[0];
      /*@
          loop_invariant (\forall int j; j >= 0 && j < h ==> states[j].value <= best.value) &&
                         (\exists int k; k >= 0 && k < h &&  states[k] == best) &&
                         h >= 0 && h <= numStates;
          loop_variant numStates - h;
        @*/
      for (int h = 1; h < numStates; h++) {
          if (states[h].value > best.value) {
              best = states[h];
          }
      }
      return best;
  }

	private static final double INITIAL_VALUE = 0.0;
	private static final int ATTACK_TYPES = 10;
    
	private static final double DEFENDER_COST = 0.5;
	private static final double RETALIATION_COST = 0.75;
	private static final double RETALIATION_EFFECT = 0.5;

  //@ invariant irrational_valid: 0.0 <= IRRATIONAL_PROBABILITY <= 1.0;
  //@ invariant attribution_valid: 0.0 <= ATTRIBUTION_PROBABILITY <= 1.0;
	private static double IRRATIONAL_PROBABILITY;
	private static double ATTRIBUTION_PROBABILITY;

  /*@ requires 0 <= intensity < ATTACK_TYPES;
      assigns \nothing;
    @ ensures \result == attackStrength_logic(intensity);
    @*/
	private static int attackStrength(int intensity) {
        return intensity + 1;
    }

  //@ invariant me_valid: state_valid(states, this, this.index);
  private int index;
	private double value;
	private int [] numIgnores;
	private int [] numRetaliates;
	private double [] retaliationTable;

  /*@ requires states_initialised(states) && 0 <= array_index < numStates &&
      0.0 <= IRRATIONAL_PROBABILITY <= 1.0 &&
      0.0 <= ATTRIBUTION_PROBABILITY <= 1.0 &&
      states_valid_up_to(states, array_index);
      assigns this.value, this.numIgnores, this.numRetaliates, this.retaliationTable, this.index, states[array_index];
    @ ensures this.index == array_index && this.value == INITIAL_VALUE && states_initialised(states) &&
      states_valid_up_to(states, array_index + 1);
    @*/
	private Deterrence(int array_index) {
    //@ assert (\forall int i; i >= 0 && i < array_index ==> states[i] != this);
    //@ assert states_valid_up_to(states, array_index);
    value = INITIAL_VALUE;
    //@ assert states_valid_up_to(states, array_index);
    numIgnores = new int[ATTACK_TYPES];
    //@ assert states_valid_up_to(states, array_index);
    numRetaliates = new int[ATTACK_TYPES];
    //@ assert states_valid_up_to(states, array_index);
    retaliationTable = new double[ATTACK_TYPES];
    //@ assert states_valid_up_to(states, array_index);

    //@ assert this.value == INITIAL_VALUE;
        
    /*@ loop_invariant (\forall int j; j >= 0 && j < i ==> numIgnores[j] >= 1 && numRetaliates[j] >= 1 && 0.0 <= retaliationTable[j] < 1.0) &&
        i >= 0 && i <= ATTACK_TYPES && states_valid_up_to(states, array_index) && states_initialised(states);
        loop_variant ATTACK_TYPES - i;
      @*/
		for (int i = 0; i < ATTACK_TYPES; i++) {
      //@ assert states_valid_up_to(states, array_index);
      retaliationTable[i] = nextDouble();
      //@ assert states_valid_up_to(states, array_index);
			numIgnores[i] = 1;
      //@ assert states_valid_up_to(states, array_index);
			numRetaliates[i] = 1;
      //@ assert states_valid_up_to(states, array_index);
		}
    //@ assert states_valid_up_to(states, array_index);

    //@ assert this.value == INITIAL_VALUE && states_initialised(states) && states_valid_up_to(states, array_index);
        
    this.index = array_index;
    states[this.index] = this;

    //@ assert this.index == array_index && this.value == INITIAL_VALUE && states_initialised(states) && states_valid_up_to(states, array_index);

    //@ assert state_valid(states, this, this.index);

    //@ assert states_valid_up_to(states, array_index + 1);
	}
    
  /*@ requires !\fresh(a) && !\fresh(b);
      assigns this.retaliationTable[0..this.retaliationTable.length-1], this.value;
      ensures value == INITIAL_VALUE;
    @*/
  private void initialiseOffspring(Deterrence a, Deterrence b) {
      value = INITIAL_VALUE;
      /*@ loop_invariant i >= 0 && i <= ATTACK_TYPES &&
          (\forall int j; j >= 0 && j < ATTACK_TYPES ==> 0.0 <= retaliationTable[j] < 1.0) &&
          (\forall int j; j >= 0 && j < ATTACK_TYPES ==> 0.0 <= a.retaliationTable[j] < 1.0) &&
          (\forall int j; j >= 0 && j < ATTACK_TYPES ==> 0.0 <= b.retaliationTable[j] < 1.0);
          loop_variant ATTACK_TYPES - i;
        @*/
      for (int i = 0; i < ATTACK_TYPES; i++) {
          retaliationTable[i] = mutate(choice(a.retaliationTable[i], b.retaliationTable[i]), nextDouble());
	    }
  }

  /*@ requires 0 <= intensity < ATTACK_TYPES && !\fresh(target) && RETALIATION_EFFECT >= 0.0;
      assigns \nothing;
      ensures \result <= attackStrength_logic(intensity) &&
      \result == expectedValue_logic(intensity, target.numIgnores[intensity], target.numRetaliates[intensity], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT);
    @*/
	private static double expectedValue(int intensity, Deterrence target) {
		return attackStrength(intensity)
        * (target.numIgnores[intensity] + target.numRetaliates[intensity] * ((1.0 - ATTRIBUTION_PROBABILITY) - RETALIATION_EFFECT * ATTRIBUTION_PROBABILITY))
        / (target.numIgnores[intensity] + target.numRetaliates[intensity]);
	}
    
  /*@ requires !\fresh(target) && RETALIATION_EFFECT >= 0.0;
      assigns \nothing;
      ensures 0 <= \result < ATTACK_TYPES &&
              (\forall int i; i >= 0 && i < ATTACK_TYPES ==>
                  expectedValue_logic(i, target.numIgnores[i], target.numRetaliates[i], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)
                  <=
                  expectedValue_logic(\result, target.numIgnores[\result], target.numRetaliates[\result], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)) &&

    @         (\exists int i; i >= 0 && i < ATTACK_TYPES &&
                  expectedValue_logic(i, target.numIgnores[i], target.numRetaliates[i], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)
                  ==
                  expectedValue_logic(\result, target.numIgnores[\result], target.numRetaliates[\result], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT));
    @*/
  private int highestExpectedValue(Deterrence target) {
      int best = 0;
      /*@
          loop_invariant
              (\forall int j; j >= 0 && j < h ==>
                  expectedValue_logic(j, target.numIgnores[j], target.numRetaliates[j], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)
                  <=
                  expectedValue_logic(best, target.numIgnores[best], target.numRetaliates[best], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)) &&
                         
              (\exists int k; k >= 0 && k < h &&
                  expectedValue_logic(k, target.numIgnores[k], target.numRetaliates[k], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)
                  ==
                  expectedValue_logic(best, target.numIgnores[best], target.numRetaliates[best], ATTRIBUTION_PROBABILITY, RETALIATION_EFFECT)) &&
              h >= 0 && h <= ATTACK_TYPES && best >= 0 && best < ATTACK_TYPES;
          loop_variant ATTACK_TYPES - h;
        @*/
      for (int h = 1; h < ATTACK_TYPES; h++) {
          if (expectedValue(h, target) > expectedValue(best, target)) {
              best = h;
          }
      }
      return best;
  }

  /*@ requires states_valid(states);
      assigns \nothing;
      ensures \result != this &&
    @         states[\result.index] == \result &&
              state_valid(states, \result, \result.index);
    @*/
  private Deterrence chooseOpponent() {
      int choice = nextInt(states.length - 1);
      //@ assert states[this.index] == this;
      if (choice >= this.index)
          choice++;
      return states[choice];
  }

	/*@ requires states_valid(states) && RETALIATION_EFFECT >= 0.0;
      ensures states_valid(states);
    @*/
	private void move() {
		Deterrence defender = chooseOpponent();
		int intensity;

		if (nextDouble() < IRRATIONAL_PROBABILITY) {
			intensity = nextInt(ATTACK_TYPES);
		} else {
			intensity = highestExpectedValue(defender);
		}
        
    //@ assert 0 <= intensity < ATTACK_TYPES;

		if (nextDouble() < defender.retaliationTable[intensity]) {
			if (nextDouble() < ATTRIBUTION_PROBABILITY) {
				this.value -= attackStrength(intensity) * RETALIATION_EFFECT;
				defender.value -= attackStrength(intensity) * RETALIATION_COST;
				defender.numRetaliates[intensity]++;
			} else {
				this.value += attackStrength(intensity);
				defender.value -= attackStrength(intensity) * RETALIATION_COST;
				defender.numRetaliates[intensity]++;
			}
		} else {
			this.value += attackStrength(intensity);
			defender.value -= attackStrength(intensity) * DEFENDER_COST;
			defender.numIgnores[intensity]++;
		}
	}

  //@ requires RETALIATION_EFFECT >= 0.0;
  public static void main(String[] args) {
    /*@ loop_invariant 0 <= att <= 11;
        loop_variant 10 - att;
    @*/
    for (int att = 0; att <= 10; att++) {
        ATTRIBUTION_PROBABILITY = att * 0.1;
        //@ assert 0.0 <= ATTRIBUTION_PROBABILITY <= 1.0;

        /*@ loop_invariant 0 <= irr <= 11;
            loop_variant 10 - irr;
        @*/
        for (int irr = 0; irr <= 10; irr++) {
            IRRATIONAL_PROBABILITY = irr * 0.1;
            //@ assert 0.0 <= IRRATIONAL_PROBABILITY <= 1.0;
            
            states = new Deterrence[numStates];

            //@ assert states_initialised(states);
            
            /*@ loop_invariant
                states_initialised(states) &&
                states_valid_up_to(states, i) &&
                i >= 0 && i <= numStates;
                loop_variant numStates - i;
              @*/
            for (int i = 0; i < numStates; i++) {
                new Deterrence(i);
            }

            //@ assert states_valid(states);

            /*@ loop_invariant states_valid(states);
                loop_variant numTurns - turn;
              @*/
            for (int turn = 0; turn < numTurns; turn++) {
                /*@ loop_invariant i >= 0 && i <= numStates && states_valid(states);
                    loop_variant numStates - i;
                  @*/
                for(int i = 0; i < numStates; i++) {
	                states[i].value = INITIAL_VALUE;
                  /*@ loop_invariant j >= 0 && j <= ATTACK_TYPES && states_valid(states);
                      loop_variant ATTACK_TYPES - j;
                    @*/
	                for (int j = 0; j < ATTACK_TYPES; j++) {
		                states[i].numIgnores[j] = 1;
		                states[i].numRetaliates[j] = 1;
	                }
                }
                /*@ loop_invariant states_valid(states);
                    loop_variant turnLength - step;
                  @*/
                for (int step = 0; step < turnLength; step++) {
                    /*@ loop_invariant i >= 0 && i <= numStates && states_valid(states);
                        loop_variant numStates - i;
                      @*/
                    for(int i = 0; i < numStates; i++) {
                        states[i].move();
                    }
                }
                
                Deterrence worst = getWorst();
                Deterrence best = getBest();
                worst.initialiseOffspring(worst, best);
            }
            
            //@ assert states_valid(states);
            //@ assert IRRATIONAL_PROBABILITY == irr * 0.1;
            //@ assert ATTRIBUTION_PROBABILITY == att * 0.1;
            printStuff(IRRATIONAL_PROBABILITY, ATTRIBUTION_PROBABILITY);
        }
    }
  }
  
  /*@ requires states_valid(states) && 0.0 <= IRRATIONAL_PROBABILITY <= 1.0 && 0.0 <= ATTRIBUTION_PROBABILITY <= 1.0;
      assigns \nothing;
  @*/
  private static void printStuff(double IRRATIONAL_PROBABILITY, double ATTRIBUTION_PROBABILITY) {
    System.out.print(IRRATIONAL_PROBABILITY);
    System.out.print(", ");
    System.out.print(ATTRIBUTION_PROBABILITY);
    System.out.print(", ");
    System.out.print(getLargestAverage());
    System.out.println();
  }
  
  /*@ requires states_valid(states);
      assigns \nothing;
  @*/
  private static double getLargestAverage() {
    double [] averages = new double [ATTACK_TYPES];
    /*@ loop_invariant i >= 0 && i <= ATTACK_TYPES;
        loop_variant ATTACK_TYPES - i;
    @*/
    for(int i = 0; i < ATTACK_TYPES; i++) {
        averages[i] = 0.0;
    }
    
    /*@ loop_invariant s >= 0 && s <= numStates;
        loop_variant numStates - s;
    @*/
    for(int s = 0; s < numStates; s++) {
        Deterrence state = states[s];
        /*@ loop_invariant i >= 0 && i <= ATTACK_TYPES && !\fresh(state);
            loop_variant ATTACK_TYPES - i;
        @*/
        for(int i = 0; i < ATTACK_TYPES; i++) {
            averages[i] += state.retaliationTable[i];
        }
    }
    
    /*@ loop_invariant i >= 0 && i <= ATTACK_TYPES;
        loop_variant ATTACK_TYPES - i;
    @*/
    for(int i = 0; i < ATTACK_TYPES; i++) {
        averages[i] /= numStates;
    }
    
    double largest = averages[0];
    /*@ loop_invariant i >= 0 && i <= ATTACK_TYPES;
        loop_variant ATTACK_TYPES - i;
    @*/
    for(int i = 1; i < ATTACK_TYPES; i++) {
        if (averages[i] > largest)
            largest = averages[i];
    }
    return largest;
  }
}
