import me.dcplugin.artifex.api.commands.CustomCommand;
import org.bukkit.entity.Player;
import org.bukkit.command.*;

/**
 * ❌ EXAMPLE OF A COMMAND WITH ERRORS (for learning)
 * 
 * This file shows COMMON MISTAKES when creating commands.
 * 
 * It will print a red message in the console:
 * [API] ❌ EXAMPLE-command-error.java:XX - error message
 * 
 * Fix the errors to make the command work!
 */
public class EXAMPLEcommandError extends CustomCommand {

    // ❌ ERROR 1: Wrong constructor
    // Correct: super("name", "desc", "/usage", "permission");
    // Mistake: super("name"); - missing parameters

    public EXAMPLEcommandError() {
        super("errorcmd", "Example with errors", "/errorcmd", "dci.error");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // ❌ ERROR 2: Using methods that do not exist
        // sendTitle(player, "title"); - there is no such method
        // Correct: title(player, "title", "subtitle");

        Player target = getTarget(sender, args, 0);
        if (target == null) return false;

        // ❌ ERROR 3: Wrong imports
        // Particle.FLAME - may not exist in older versions
        // Correct: use try-catch

        // ❌ ERROR 4: Missing checks
        // target.setHealth(100); - may cause an error if > maxHealth

        // ❌ ERROR 5: Slow operations in the main thread
        // Thread.sleep(1000); - NEVER do this!
        // Correct: use the scheduler

        // ❌ ERROR 6: Wrong return
        // return; - does not work in the void execute method
        // Correct: return true;

        return true;
    }
}

/**
 * HOW TO FIX:
 * 
 * 1. Make sure all methods exist
 * 2. Check the imports
 * 3. Use try-catch for risky operations
 * 4. Check the bounds (health, food, level)
 * 5. Do not block the main thread
 * 6. Return true/false correctly
 */
