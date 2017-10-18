package actions;

import main.Game;

public class ListCropsAction extends Action {

    public ListCropsAction(Game game) {
        super(game);
    }

    @Override
    public void execute() {
        game.listCrops();
    }

    @Override
    public String getPrompt() {
        return "List available crops for purchase";
    }

}
