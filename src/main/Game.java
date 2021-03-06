package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import actions.Action;
import actions.BuyCropsAction;
import actions.BuyFieldsAction;
import actions.ExitAction;
import actions.ListCropsAction;
import actions.PlayAction;
import actions.StatusAction;

public class Game {
    
	/*
	 * Setting determining the length of the game.
	 */
	private static final int END_GAME_YEAR = 20;
	
    /*
     * Normally distributed about 1.0 +/- 0.1 with cutoffs at +/- 3 st devs.
     */
    private static final double WETNESS_DEVIATION = 0.1;
    private static final double WETNESS_MIN = 1 - 3 * WETNESS_DEVIATION;
    private static final double WETNESS_MAX = 1 + 3 * WETNESS_DEVIATION;

    /*
     * Normally distributed about 1.0 +/- 0.1 with cutoffs at +/- 3 st devs.
     */
    private static final double HEAT_DEVIATION = 0.1;
    private static final double HEAT_MIN = 1 - 3 * HEAT_DEVIATION;
    private static final double HEAT_MAX = 1 + 3 * HEAT_DEVIATION;
    
    /*
     * Heat intervals for weather reporting between minimum and maximum
     */
    private static final WeatherBand[] HEAT_BANDS = new WeatherBand[] {
            new WeatherBand(-3.0, "This was a glacial year "),
            new WeatherBand(-2.5, "This was a freezing year "),
            new WeatherBand(-2.0, "This was a frigid year "),
            new WeatherBand(-1.5, "This was a bracing year "),
            new WeatherBand(-1.0, "This was a chilly year "),
            new WeatherBand(-0.5, "This was a mild year "),
            new WeatherBand(0.5, "This was a warm year "),
            new WeatherBand(1.0, "This was a hot year "),
            new WeatherBand(1.5, "This was a sultry year "),
            new WeatherBand(2.0, "This was a sweltering year "),
            new WeatherBand(2.5, "This was a scorching year "),
    };
    
    /*
     * Wetness intervals for weather reporting between minimum and maximum
     */
    private static final WeatherBand[] WETNESS_BANDS = new WeatherBand[] {
            new WeatherBand(-3.0, "with an arid climate."),
            new WeatherBand(-2.5, "with minimal precipitation."),
            new WeatherBand(-2.0, "with scattered drizzle."),
            new WeatherBand(-1.5, "with scarce rainfall."),
            new WeatherBand(-1.0, "with light showers."),
            new WeatherBand(-0.5, "with moderate rainfall."),
            new WeatherBand(0.5, "with considerable precipitation."),
            new WeatherBand(1.0, "with heavy rainfall."),
            new WeatherBand(1.5, "with some squalling."),
            new WeatherBand(2.0, "with torrential downpours."),
            new WeatherBand(2.5, "with monsoon storms."), 
    };
    
    /**
     * The player's current balance.
     */
	private int money = 500;
	
	/**
	 * The player's yearly expenses.
	 */
	private int expenditure = 0;
	
	/**
	 * The player's assets acquired this year (for reporting).
	 */
	private int newAssets = 0;
	
	/**
	 * The current year.
	 */
	private int year = 1;
	
	/**
	 * The player's final score.
	 */
	private int score = 0;
	
	private List<Field> availableFields;
	private List<Crop> crops;
	private int lowestCropPrice;
    
    private List<Field> playerFields = new ArrayList<>();
    
    private boolean exiting;
    
    private Console console;
    
    private Random rand;
    private InputProvider inputProvider;
    
    public Game(long seed, InputProvider inputProvider, Console console, 
            List<Crop> crops, List<Field> fields) {
        
        this.console = console;
        this.crops = crops;
        this.availableFields = new ArrayList<>(fields);
        
        rand = new Random(seed);
        this.inputProvider = inputProvider;
        
        // Give player initial field
        playerFields.add(availableFields.get(0));
        availableFields.remove(0);
        
        lowestCropPrice = Integer.MAX_VALUE;
        for (Crop crop : crops) {
            if (crop.getCost() < lowestCropPrice) {
                lowestCropPrice = crop.getCost();
            }
        }
    }
    
    /**
     * The game loop. Returns the player's score.
     */
    public int run() {
    	
        introduce();
        
        while (!exiting){
        	
        	/**
        	 * End game if player has no remaining funds.
        	 */
            if (!canAffordCrops()) {
            	console.print("You are bankrupt. You will have to find a job.");
            	console.newLine();
            	break;
            }
        	
        	pollInput();
            
            if (exiting) {
                break;
            }
            
            processGame();
            
        	if (year - 1 == END_GAME_YEAR) {
        		evaluateScore();
        		break;
        	}
        }
        
        finish();
        return score;
        
    }
    
    /**
     * Performs any clean-up before exiting the game.
     */
    private void finish() {
        console.print("Bye!");
        inputProvider.close();
    }
    
    /**
     * Explains the game to the player.
     */
    private void introduce() {
        console.print("Welcome to Agricultural Capitalism Simulator!");
        console.newLine();
        console.print("You have " + END_GAME_YEAR + " years to make "
        		+ "maximum profit.");
        console.newLine();
        console.sectionBreak();
        console.newLine();
    }
    
    /**
     * Prompts the player for all necessary input for the round.
     */
    private void pollInput() {
    	
        while (true) {
            
            List<Action> actions = new ArrayList<Action>();
            
            actions.add(new ListCropsAction(this));
            actions.add(new StatusAction(this));
            
            if (isEmptyFieldAvailable() && canAffordCrops()) {
                actions.add(new BuyCropsAction(this));
            }
            
            if (availableFields.size() != 0) {
            	actions.add(new BuyFieldsAction(this));
            }
            
            actions.add(new PlayAction(this));
            actions.add(new ExitAction(this));
            
            console.print("What would you like to do?");
            console.newLine();
            
            for (int i = 0; i < actions.size(); i++) {
                Action action = actions.get(i);
                console.print((i + 1) + ") " + action.getPrompt());
            }
            console.newLine();
            
            Action action = inputProvider.getNextAction(actions, this);
            action.execute();
            
            if (action.shouldEndRound()) {
                break;
            }
            
            inputProvider.waitForEnter();
            console.sectionBreak();
            console.newLine();
        }
    }
    
    private boolean canAffordCrops() {
        return money > lowestCropPrice;
    }

    /**
     * 
     */
    public boolean isEmptyFieldAvailable() {
        
        for (Field field : playerFields) {
            if (field.isEmpty()) {
                return true;                
            }
        }
        
        return false;
    }
    
    /**
     * Allow player to purchase and plant crops in available fields.
     */
    public void buyCrops() {
                
        List<Field> emptyFields = playerFields.stream()
                .filter(f -> f.isEmpty())
                .collect(Collectors.toList());
        
        console.print("Your available fields:");
        
        for (int i = 0; i < emptyFields.size(); i++) {
            console.print((i + 1) + ") " + emptyFields.get(i).getName());
        }
        
        console.newLine();
        console.print("Which field would you like to plant in?");
        
        Field field = inputProvider.getFieldToPlant(emptyFields);
        
        console.newLine();
        console.print("Available crops for planting:");

        List<Crop> affordableCrops = crops
                .stream()
                .filter(c -> (c.getCost() <= money))
                .collect(Collectors.toList());
        
        for (int i = 0; i < affordableCrops.size(); i++) {
            console.print(
                    (i + 1) + ") " + 
                    affordableCrops.get(i).getName() + ", " + 
                    affordableCrops.get(i).getCost() + " per unit");
        }
        
        console.newLine();
        console.print("Which crop would you like to plant?");
        
        Crop crop = inputProvider.getCropToPlant(field, money, affordableCrops);
        
        // Can't exceed field capacity or spend more money than we have
        int maxVolume = Math.min(
                field.getMaxCropQuantity(),
                (int) (money / crop.getCost()));
        
        console.newLine();
        console.print("How many units would you like to purchase (maximum " 
                + maxVolume + ")?");
        console.print("Enter 0 to exit to menu.");
        
        int quantity = inputProvider.getCropQuantity(maxVolume);
        
        console.newLine();
        
        if (quantity == 0) {
            return;
        }
        
        field.setCrop(crop);
        field.setCropQuantity(quantity);
        
        int totalCost = quantity * crop.getCost();
        
        money -= totalCost;
        expenditure += totalCost;
    }
    
    /**
     * Allow player to buy new fields
     */
    public void buyFields() {
    	
    	console.print("Here are the fields available for purchase:");
    	console.newLine();
    	
    	for (int i = 0; i < availableFields.size(); i++) {
    		console.print((i + 1) + ") " + 
    		        availableFields.get(i).getName() + ", " +
    				availableFields.get(i).getDescription());
    		console.print("Price: " + availableFields.get(i).getPrice());
    		console.newLine();
    	}
    	
    	console.print("Which field would you like to purchase? Enter " +
    			(availableFields.size() + 1) + " to return to menu.");
    	
    	Field field = inputProvider.getFieldToBuy(availableFields);
    	
    	console.newLine();

        // If player has selected the return command, return to menu
        if (field == null) {
            return;
        }
    	
    	if (field.getPrice() > money) {
    		console.print("Sorry, you have insufficient funds.");
    		return;
    	}
    	
		// Add to player's fields, mark it owned, deduct money
		playerFields.add(field);
        int price = field.getPrice();
		availableFields.remove(field);
		
		money -= price;
		expenditure += price;
		newAssets += price;
    	
    	console.newLine();
    }
    
    /**
     * List crops that are available for purchase.
     */
    public void listCrops() {
    	for (Crop crop : crops) {
    		console.print("**" + crop.getName());
    		console.print(crop.getDescription());
    		console.print("Cost: " + crop.getCost());
    		console.print("Sale Price: " + crop.getSalePrice());
    		console.newLine();
    	}
    }
    
    /**
     * Report farm status to player.
     */
    public void reportStatus() {
        
        console.print("Year: " + year);
        console.print("Balance: " + money);
        console.print("Asset value: " + calculateAssets());
        
        console.newLine();
        
        console.print("Fields:");
        
        for (Field field: playerFields) {
            
            Crop crop = field.getCrop();
            
            if (crop == null) {
                console.print(field.getName() + ", size " +
                		field.getMaxCropQuantity() + ", value " + 
                		field.getPrice() + ", is empty!");
            } else {
                console.print(field.getName() + ", size " +
                		field.getMaxCropQuantity() + ", value " + 
                		field.getPrice() + " - " + 
                		crop.getName() + " (" +
                        field.getCropQuantity() + " / " +
                        field.getMaxCropQuantity() + ")");
            }
        }

        console.newLine();
    }
    
    /**
     * Performs the end-of-round logic.
     */
    private void processGame() {
        
        double wetness = generateWetness();
        double heat = generateHeat();
        int profit = calculateProfit(wetness, heat);
        
        money += profit;
        year++;
        
        showResults(wetness, heat, profit);
        
        for (Field field : playerFields) {
            field.clear();
        }
    }
    
    /**
     * Generates a random value for wetness.
     * 
     * @return
     */
    private double generateWetness() {
    	double wetness = 0;
    	
    	while (wetness < WETNESS_MIN || wetness > WETNESS_MAX) {
    		wetness = rand.nextGaussian() * WETNESS_DEVIATION + 1;
    	}
    	
    	return wetness;
    }
    
    /**
     * Generates a random value for heat.
     * 
     * @return
     */
    private double generateHeat() {
    	double heat = 0;
    	
    	while (heat < HEAT_MIN || heat > HEAT_MAX) {
    		heat = rand.nextGaussian() * HEAT_DEVIATION + 1;
    	}
    	
    	return heat;
    }
    
    /**
     * Determines the profit for the round.
     * 
     * @param wetness
     * @param heat
     * @return
     */
    private int calculateProfit(double wetness, double heat) {
    	
        int profit = 0;
        
        for (Field field : playerFields) {
            
            Crop crop = field.getCrop();
            
            if (crop == null) {
                // Field is empty!
                continue;
            }
            
            /*
             * Evaluate the distance between the crop's ideal weather and the
             * actual weather, scale this depending on the crop's sensitivity
             * to that weather, and calculate yield as a perfect score of 1
             * minus deductions according to weather differences.
             */
             
            double heatDelta = Math.abs(heat - crop.getIdealHeat());
            double wetnessDelta = Math.abs(wetness - crop.getIdealWetness());
            
            double heatScore = heatDelta * crop.getHeatFactor();
            double wetnessScore = wetnessDelta * crop.getWetnessFactor();
            
            double yield = 1 - heatScore - wetnessScore;
            
            int fieldRevenue = (int) (yield * field.getCropQuantity()
            		* crop.getSalePrice() * field.getSoilQuality());
            
            field.setLastRevenue(fieldRevenue);
            
            profit += fieldRevenue;
            
        }
        
        return profit;
    }
    
    /**
     * Return the total value of the player's assets.
     * 
     * @return
     */
    private int calculateAssets() {
    	
    	int assets = 0;
    	
    	for (Field field : playerFields) {
    		assets += field.getPrice();
    	}
    	
    	return assets;
    	
    }
    
    /**
     * Report the year's weather in plain English.
     * 
     * @param wetness
     * @param heat
     */
    private void reportWeather(double wetness, double heat) {
    	    	
    	String report = checkBands(heat, HEAT_DEVIATION, HEAT_BANDS);
    	report += checkBands(wetness, WETNESS_DEVIATION, WETNESS_BANDS);
    	
    	console.print(report);
    	
    }
    
    /**
     * Return the appropriate in-game description for a weather variable.
     * 
     * @param value
     * @param deviation
     * @param bands
     * @return
     */
    private String checkBands(double value, double deviation, 
            WeatherBand[] bands) {
        
        String report = "";
        
        for (int i = bands.length - 1; i > -1; i--) {
            double thisHeatBand = bands[i].getMinValue();
            
            if (value >= (1 + deviation * thisHeatBand)) {
                report = bands[i].getMessage();
                break;
            }
        }
        
        return report;
        
    }
    
    /**
     * Report the performance of each field in turn 
     */
    private void reportFieldPerformance() {
    	
    	for (Field field : playerFields) {
    		
    		// Skip fields that had no crops planted
    		if (field.isEmpty()) {
    			continue;
    		};
    		
    		int fieldExpense = field.getCrop().getCost() * 
    				field.getCropQuantity();
    		
    		String fieldCrop = field.getCrop().getName();
    		
    		console.print(field.getName() + " - " + fieldCrop + ", Revenue " + 
    				field.getLastRevenue() + ", Cost " + fieldExpense);
    		
    	}
    	
    }
    
    /**
     * Displays the results of the round and the current state of the game.
     * 
     * @param heat 
     * @param wetness 
     * @param profit 
     */
    private void showResults(double wetness, double heat, int profit) {
        
    	console.sectionBreak();
    	reportWeather(wetness, heat);
    	console.newLine();
    	    	
    	int netProfit = profit + newAssets - expenditure;
    	
    	console.print("Year " + (year - 1) + " performance:");
    	console.newLine();
    	reportFieldPerformance();
    	console.newLine();
    	console.print("Asset acquisitions: " + newAssets);
    	console.print("Revenue: " + profit);
    	console.print("Expenses: " + expenditure);
    	console.newLine();
    	console.print("---------------------");
    	
        if (netProfit > 0) {
            console.print("Congratulations! You made a net profit of " + 
            		netProfit + ".");
        } else if (netProfit == 0) {
        	console.print("It could be worse; you broke even.");
        } else {
            console.print("Commiserations! You made a loss of " + netProfit + 
            		".");
        }
        
        expenditure = 0;
        newAssets = 0;
        
        console.print("Year end balance: " + money);
        console.print("Total asset value: " + calculateAssets());
        console.sectionBreak();
        console.newLine();
        inputProvider.waitForEnter();
                
    }
    
    /**
     * Calculate and report the player's final score.
     */
    private void evaluateScore() {
    	
    	score = money + calculateAssets();
    	
    	console.sectionBreak();
    	console.sectionBreak();
    	console.newLine();
    	console.print("Final score: " + score);
    	console.newLine();
    	console.print("Well played, capitalist! The rich get richer.");
    	console.newLine();
    	
    }

    public void exit() {
        exiting = true;
    }

    public List<Field> getAvailableFields() {
        return availableFields;
    }

    public int getMoney() {
        return money;
    }
    
}
