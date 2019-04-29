package world.bentobox.a2b.commands;

import java.util.List;

import world.bentobox.a2b.A2B;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class AdminCommand extends CompositeCommand {

    public AdminCommand(A2B addon) {
        super(addon, "a2b");
    }

    @Override
    public void setup() {
        setPermission("admin.*");
        setOnlyPlayer(false);
        setParametersHelp("a2b.commands.admin.help.parameters");
        setDescription("a2b.commands.admin.help.description");
        // Conversion
        new AdminConvertCommand(this);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            user.sendMessage("general.errors.unknown-command", TextVariables.LABEL, getTopLabel());
            return false;
        }
        // By default run the attached help command, if it exists (it should)
        return showHelp(this, user);
    }

}
