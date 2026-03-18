package io.github.beeebea.fastmove.client;

import io.github.beeebea.fastmove.IFastMoveInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FastMoveInput implements IFastMoveInput {
    private final KeyMapping moveUpKey;
    private final KeyMapping moveDownKey;
    private boolean moveUpKeyPressed = false;
    private boolean moveDownKeyPressed = false;
    private boolean moveUpKeyPressedLastTick = false;
    private boolean moveDownKeyPressedLastTick = false;

    public FastMoveInput(KeyMapping moveUpKey, KeyMapping moveDownKey) {
        this.moveUpKey = moveUpKey;
        this.moveDownKey = moveDownKey;
    }

    public void onEndTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        moveUpKeyPressedLastTick = moveUpKeyPressed;
        moveDownKeyPressedLastTick = moveDownKeyPressed;

        if (!moveUpKey.isUnbound()) {
            moveUpKeyPressed = moveUpKey.isDown();
            while (moveUpKey.consumeClick()) {
                moveUpKeyPressed = true;
            }
        } else {
            moveUpKeyPressed = player.input.jumping;
        }

        if (!moveDownKey.isUnbound()) {
            moveDownKeyPressed = moveDownKey.isDown();
            while (moveDownKey.consumeClick()) {
                moveDownKeyPressed = true;
            }
        } else {
            moveDownKeyPressed = player.input.shiftKeyDown;
        }
    }

    @Override
    public boolean ismoveUpKeyPressed() {
        return moveUpKeyPressed;
    }

    @Override
    public boolean ismoveDownKeyPressed() {
        return moveDownKeyPressed;
    }

    @Override
    public boolean ismoveUpKeyPressedLastTick() {
        return moveUpKeyPressedLastTick;
    }

    @Override
    public boolean ismoveDownKeyPressedLastTick() {
        return moveDownKeyPressedLastTick;
    }
}
