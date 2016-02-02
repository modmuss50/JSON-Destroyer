package me.modmuss50.jsonDestroyer.api;

import net.minecraftforge.fluids.Fluid;

/**
 * Use this to generate a forge bucket model for a bucket item
 */
public interface ITexturedBucket extends IDestroyable {

    /**
     * @param damage the damage of the item that the texture is being loaded for
     * @return if the bucket model should be flipped because its a gas
     */
    boolean isGas(int damage);

    /**
     * @param damage the damage of the item that the texture is being loaded for
     * @return the fluid
     */
    Fluid getFluid(int damage);

    /**
     * @return Return 1 if basic item, else return the max damage
     */
    int getMaxMeta();


}
