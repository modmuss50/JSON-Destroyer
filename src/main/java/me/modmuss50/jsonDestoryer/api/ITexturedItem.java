package me.modmuss50.jsonDestoryer.api;

/**
 * Use this to apply a texture to an item
 */
public interface ITexturedItem extends IDestroyable {

    /**
     * @param damage the damage of the item that the texture is being loaded for
     * @return the Texture name in the form of "modid:items/textureName"
     */
    String getTextureName(int damage);

    /**
     * @return Return 0 if basic item, else return the max damage
     */
    int getMaxDamage();
}
