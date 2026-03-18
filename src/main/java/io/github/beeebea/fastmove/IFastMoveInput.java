package io.github.beeebea.fastmove;

public interface IFastMoveInput {
    default boolean ismoveUpKeyPressed() { return false; }
    default boolean ismoveDownKeyPressed() { return false; }
    default boolean ismoveUpKeyPressedLastTick() { return false; }
    default boolean ismoveDownKeyPressedLastTick() { return false; }
}
