package me.modmuss50.jsonDestoryer.api;

/**
 * Add this to your fluid class to give it a texture
 */
public interface ITexturedFluid extends IDestroyable {

    /**
     * @return The texture name
     */
    String getTextureName();
}
