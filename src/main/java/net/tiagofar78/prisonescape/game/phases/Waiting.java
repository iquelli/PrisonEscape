package net.tiagofar78.prisonescape.game.phases;

public class Waiting extends Phase {

    @Override
    public Phase next() {
        return new Ongoing();
    }

    @Override
    public boolean isClockStopped() {
        return true;
    }

    @Override
    public boolean hasGameStarted() {
        return false;
    }

    @Override
    public boolean hasGameEnded() {
        return false;
    }

    @Override
    public boolean isGameDisabled() {
        return false;
    }
}
