package ac.boar.anticheat.check.impl.timer;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.player.BoarPlayer;

@Experimental
@CheckInfo(name = "Timer")
public final class Timer extends Check {
    private static final long AVERAGE_DISTANCE = (long) 5e+7;

    private long lastNS, balance, lastTick;

    public Timer(final BoarPlayer player) {
        super(player);
    }

    public boolean tick(long tick) {
        if (this.lastNS == 0 || player.inLoadingScreen || player.sinceLoadingScreen < 20) {
            this.lastNS = System.nanoTime();
            this.lastTick = tick;
            this.balance = 0;
            return false;
        }

        if (player.tick == Long.MIN_VALUE) {
            player.tick = this.lastTick;
        } else {
            long distanceToPrev = tick - player.tick;
            if (distanceToPrev != 1) {
                player.kick("Invalid tick id=" + tick);
                return true;
            }
        }

        long distance = System.nanoTime() - this.lastNS;
        long neededDistance = (tick - this.lastTick) * AVERAGE_DISTANCE;

        boolean valid = true;
        // GeyserBoar.getLogger().info("New balance: " + this.balance + "," + distance);
        if (this.balance > AVERAGE_DISTANCE + 1e+7 + 3e+6) {
            this.fail("balance=" + this.balance + ", player is ahead!");
            player.getTeleportUtil().teleportTo(player.getTeleportUtil().getLastKnowValid());
            this.balance -= AVERAGE_DISTANCE;
            valid = false;
        }

        int predictedTickAdvance = (int) (distance / AVERAGE_DISTANCE);
        if (distance > AVERAGE_DISTANCE) {
            player.tick += Math.min(tick - this.lastTick, predictedTickAdvance);
        } else {
            player.tick += 1;
        }

        if (player.tick != tick) {
            player.kick("Invalid tick id=" + tick);
            return true;
        }

//        if (distance > AVERAGE_DISTANCE + 1e+7 && this.balance < 0) {
//            // fail("balance=" + this.balance + ", player is behind!");
//            this.balance += AVERAGE_DISTANCE * (predictedTickAdvance - (tick - this.lastTick) - 1);
//        } else {
//        }
        this.balance -= distance - neededDistance;

        this.lastNS = System.nanoTime();
        this.lastTick = player.tick;
        return !valid;
    }
}
