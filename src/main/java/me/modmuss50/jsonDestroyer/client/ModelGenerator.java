package me.modmuss50.jsonDestroyer.client;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.modmuss50.jsonDestroyer.JsonDestroyer;
import me.modmuss50.jsonDestroyer.api.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.*;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.vector.Vector3f;

import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ModelGenerator {

    private FaceBakery faceBakery = new FaceBakery();

    private JsonDestroyer jsonDestroyer;

    public ArrayList<BlockIconInfo> blockIconInfoList = new ArrayList<BlockIconInfo>();

    public HashMap<BlockIconInfo, TextureAtlasSprite> blockIconList = new HashMap<BlockIconInfo, TextureAtlasSprite>();
    public HashMap<BlockFluidBase, TextureAtlasSprite> fluidIcons = new HashMap<BlockFluidBase, TextureAtlasSprite>();
    public List<ItemIconInfo> itemIcons = new ArrayList<ItemIconInfo>();

    public ModelGenerator(JsonDestroyer jsonDestroyer) {
        this.jsonDestroyer = jsonDestroyer;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void textureStitch(TextureStitchEvent.Pre event) {
        TextureMap textureMap = event.map;
        for (Object object : jsonDestroyer.objectsToDestroy) {
            if (object instanceof Block && object instanceof ITexturedBlock) {
                ITexturedBlock blockTextureProvider = (ITexturedBlock) object;
                Block block = (Block) object;
                for (int i = 0; i < blockTextureProvider.amountOfStates(); i++) {
                    for (EnumFacing side : EnumFacing.values()) {
                        String name;
                        name = blockTextureProvider.getTextureNameFromState(block.getStateFromMeta(i), side);
                        TextureAtlasSprite texture = textureMap.getTextureExtry(name);
                        if (texture == null) {
                            texture = new CustomTexture(name);
                            textureMap.setTextureEntry(name, texture);
                        }
                        BlockIconInfo iconInfo = new BlockIconInfo(block, i, side);
                        blockIconList.put(iconInfo, texture);
                        blockIconInfoList.add(iconInfo);
                    }
                }
            } else if (object instanceof BlockFluidBase && object instanceof ITexturedFluid) {
                ITexturedFluid fluidTextureProvider = (ITexturedFluid) object;
                String name = fluidTextureProvider.getTextureName();
                TextureAtlasSprite texture = textureMap.getTextureExtry(name);
                if (texture == null) {
                    texture = new CustomTexture(name);
                    textureMap.setTextureEntry(name, texture);
                }
                fluidIcons.put((BlockFluidBase) object, texture);
            } else if (object instanceof Item && object instanceof ITexturedItem) {
                ITexturedItem itemTexture = (ITexturedItem) object;
                Item item = (Item) object;
                for (int i = 0; i < itemTexture.getMaxMeta(); i++) {
                    String name = itemTexture.getTextureName(i);
                    TextureAtlasSprite texture = textureMap.getTextureExtry(name);
                    if (texture == null) {
                        texture = new CustomTexture(name);
                        textureMap.setTextureEntry(name, texture);
                    }
                    ItemIconInfo info = new ItemIconInfo((Item) object, i, texture, name);
                    itemIcons.add(info);
                }
            } else if (object instanceof Item && object instanceof ITexturedBucket) {
                ITexturedBucket itemTexture = (ITexturedBucket) object;
                for (int i = 0; i < itemTexture.getMaxMeta(); i++) {
                    String name = itemTexture.getFluid(i).getStill().toString();
                    TextureAtlasSprite texture = textureMap.getTextureExtry(name);
                    if (texture == null) {
                        texture = new CustomTexture(name);
                        textureMap.setTextureEntry(name, texture);
                    }
                    ItemIconInfo info = new ItemIconInfo((Item) object, i, texture, name);
                    info.isBucket = true;
                    itemIcons.add(info);
                }
            }
        }

    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void bakeModels(ModelBakeEvent event) {
        ItemModelMesher itemModelMesher = Minecraft.getMinecraft().getRenderItem().getItemModelMesher();
        for (Object object : jsonDestroyer.objectsToDestroy) {
            if (object instanceof Block && object instanceof ITexturedBlock) {
                ITexturedBlock textureProvdier = (ITexturedBlock) object;
                Block block = (Block) object;
                for (int i = 0; i < textureProvdier.amountOfStates(); i++) {
                    HashMap<EnumFacing, TextureAtlasSprite> textureMap = new HashMap<EnumFacing, TextureAtlasSprite>();
                    for (EnumFacing side : EnumFacing.VALUES) {
                        for (BlockIconInfo iconInfo : blockIconInfoList) {
                            if (iconInfo.getBlock() == block && iconInfo.getMeta() == i && iconInfo.getSide() == side) {
                                if (blockIconList.containsKey(iconInfo))
                                    textureMap.put(side, blockIconList.get(iconInfo));
                            }
                        }
                    }
                    if (textureMap.isEmpty()) {
                        return;
                    }

                    ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> map = IPerspectiveAwareModel.MapWrapper.getTransforms(ModelRotation.X0_Y0);
                    BlockModel model = new BlockModel(textureMap, block.getStateFromMeta(i).getBlock() instanceof IOpaqueBlock, map);
                    //IBakedModel model = new IPerspectiveAwareModel.MapWrapper(model1, ModelRotation.X0_Y0);
                    ModelResourceLocation modelResourceLocation = getModelResourceLocation(block.getStateFromMeta(i));

                    event.getModelRegistry().putObject(modelResourceLocation, model);
                    ModelResourceLocation inventory = getBlockinventoryResourceLocation(block);
                    event.getModelRegistry().putObject(inventory, model);
                    itemModelMesher.register(Item.getItemFromBlock(block), i, inventory);
                    event.getModelRegistry().putObject(modelResourceLocation, model);
                    itemModelMesher.register(Item.getItemFromBlock(block), i, modelResourceLocation);
                }
            } else if (object instanceof ITexturedFluid && object instanceof BlockFluidBase) {
                final BlockFluidBase fluid = (BlockFluidBase) object;
                final ModelResourceLocation fluidLocation = new ModelResourceLocation(fluid.getFluid().getFlowing(), "fluid");

                Item fluidItem = Item.getItemFromBlock(fluid);
                ModelBakery.registerItemVariants(fluidItem);
                ModelLoader.setCustomMeshDefinition(fluidItem, new ItemMeshDefinition() {
                    public ModelResourceLocation getModelLocation(ItemStack stack) {
                        return fluidLocation;
                    }
                });
                ModelLoader.setCustomStateMapper(fluid, new StateMapperBase() {
                    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                        return fluidLocation;
                    }
                });

                for (int i = 0; i < 16; i++) {
                    ModelResourceLocation location = new ModelResourceLocation(getBlockResourceLocation(fluid), "level=" + i);
                    ModelFluid modelFluid = new ModelFluid(fluid.getFluid());
                    Function<ResourceLocation, TextureAtlasSprite> textureGetter = new Function<ResourceLocation, TextureAtlasSprite>() {
                        public TextureAtlasSprite apply(ResourceLocation location) {
                            return fluidIcons.get(fluid);
                        }
                    };
                    IBakedModel bakedModel = modelFluid.bake(modelFluid.getDefaultState(), DefaultVertexFormats.BLOCK, textureGetter);

                    event.getModelRegistry().putObject(location, bakedModel);
                }
                ModelResourceLocation inventoryLocation = new ModelResourceLocation(getBlockResourceLocation(fluid), "inventory");
                ModelFluid modelFluid = new ModelFluid(fluid.getFluid());
                Function<ResourceLocation, TextureAtlasSprite> textureGetter = new Function<ResourceLocation, TextureAtlasSprite>() {
                    public TextureAtlasSprite apply(ResourceLocation location) {
                        return fluidIcons.get(fluid);
                    }
                };
                IBakedModel bakedModel = modelFluid.bake(modelFluid.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter);

                event.getModelRegistry().putObject(inventoryLocation, bakedModel);
            } else if (object instanceof Item && object instanceof ITexturedItem) {
                ITexturedItem iTexturedItem = (ITexturedItem) object;
                Item item = (Item) object;
                for (int i = 0; i < iTexturedItem.getMaxMeta(); i++) {
                    TextureAtlasSprite texture = null;
                    ItemIconInfo itemIconInfo = null;
                    for (ItemIconInfo info : itemIcons) {
                        if (info.damage == i && info.getItem() == item && info.isBucket == false) {
                            texture = info.getSprite();
                            itemIconInfo = info;
                            break;
                        }
                    }
                    if (texture == null) {
                        break;
                    }

                    ModelResourceLocation inventory;
                    inventory = getItemInventoryResourceLocation(item);

                    //TODO 1.9
                    if (iTexturedItem.getMaxMeta() != 1) {
                        if (iTexturedItem.getModel(new ItemStack(item, 1, i), Minecraft.getMinecraft().thePlayer, 0) != null) {
                            inventory = iTexturedItem.getModel(new ItemStack(item, 1, i), Minecraft.getMinecraft().thePlayer, 0);
                            ModelLoader.registerItemVariants(item, inventory);
                        }
                    }


                    final TextureAtlasSprite finalTexture = texture;
                    Function<ResourceLocation, TextureAtlasSprite> textureGetter = new Function<ResourceLocation, TextureAtlasSprite>() {
                        public TextureAtlasSprite apply(ResourceLocation location) {
                            return finalTexture;
                        }
                    };
                    ImmutableList.Builder<ResourceLocation> builder = ImmutableList.builder();
                    builder.add(new ResourceLocation(itemIconInfo.textureName));
                    ItemLayerModel itemLayerModel = new ItemLayerModel(builder.build());
                    IBakedModel model = itemLayerModel.bake(ItemLayerModel.instance.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter);
                    itemModelMesher.register(item, i, inventory);
                    event.getModelRegistry().putObject(inventory, model);
                }
            } else if (object instanceof Item && object instanceof ITexturedBucket) {
                ITexturedBucket iTexturedBucket = (ITexturedBucket) object;
                Item item = (Item) object;
                for (int i = 0; i < iTexturedBucket.getMaxMeta(); i++) {
                    ModelResourceLocation inventory;
                    inventory = getItemInventoryResourceLocation(item);
//                    if (iTexturedBucket.getMaxMeta() != 1) {
//                        //TODO 1.9
//                        if (item.getModel(new ItemStack(item, 1, i), Minecraft.getMinecraft().thePlayer, 0) != null) {
//                            inventory = item.getModel(new ItemStack(item, 1, i), Minecraft.getMinecraft().thePlayer, 0);
//                        }
//                    }
                    Function<ResourceLocation, TextureAtlasSprite> textureGetter;
                    textureGetter = new Function<ResourceLocation, TextureAtlasSprite>() {
                        public TextureAtlasSprite apply(ResourceLocation location) {
                            return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString());
                        }
                    };
                    ModelDynBucket modelDynBucket = new ModelDynBucket(new ResourceLocation("forge:items/bucket_base"), new ResourceLocation("forge:items/bucket_fluid"), new ResourceLocation("forge:items/bucket_cover"), iTexturedBucket.getFluid(i), iTexturedBucket.isGas(i));

                    IBakedModel model = modelDynBucket.bake(ItemLayerModel.instance.getDefaultState(), DefaultVertexFormats.ITEM, textureGetter);
                    itemModelMesher.register(item, i, inventory);
                    event.getModelRegistry().putObject(inventory, model);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static ModelResourceLocation getModelResourceLocation(IBlockState state) {
        return new ModelResourceLocation((ResourceLocation) Block.blockRegistry.getNameForObject(state.getBlock()), (new DefaultStateMapper()).getPropertyString(state.getProperties()));
    }

    @SideOnly(Side.CLIENT)
    public static ModelResourceLocation getBlockinventoryResourceLocation(Block block) {
        return new ModelResourceLocation(Block.blockRegistry.getNameForObject(block), "inventory");
    }

    @SideOnly(Side.CLIENT)
    public static ModelResourceLocation getItemInventoryResourceLocation(Item block) {
        return new ModelResourceLocation(Item.itemRegistry.getNameForObject(block), "inventory");
    }

    @SideOnly(Side.CLIENT)
    public static ResourceLocation getBlockResourceLocation(Block block) {
        return Block.blockRegistry.getNameForObject(block);
    }

    public class CustomTexture extends TextureAtlasSprite {
        public CustomTexture(String spriteName) {
            super(spriteName);
        }
    }

    //BLOCK

    public class BlockIconInfo {
        public Block block;
        public int meta;
        public EnumFacing side;

        public BlockIconInfo(Block block, int meta, EnumFacing side) {
            this.block = block;
            this.meta = meta;
            this.side = side;
        }

        public Block getBlock() {
            return block;
        }

        public int getMeta() {
            return meta;
        }

        public EnumFacing getSide() {
            return side;
        }

    }

    public class BlockModel implements IPerspectiveAwareModel {
        HashMap<EnumFacing, TextureAtlasSprite> textureAtlasSpriteHashMap;
        boolean isOpaque;
        ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms;

        public BlockModel(HashMap<EnumFacing, TextureAtlasSprite> textureAtlasSpriteHashMap, boolean isOpaque, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms) {
            this.textureAtlasSpriteHashMap = textureAtlasSpriteHashMap;
            this.isOpaque = isOpaque;
            this.transforms = transforms;
        }


        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
            ArrayList<BakedQuad> list = new ArrayList<BakedQuad>();
            BlockFaceUV uv = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);
            BlockPartFace face = new BlockPartFace(null, 0, "", uv);
            ModelRotation modelRot = ModelRotation.X0_Y0;
            boolean scale = true;
            if(side != null && !isOpaque){
                switch (side) {
                    case DOWN:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 0.0F, 16.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//down
                        break;
                    case UP:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 16.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//up
                        break;
                    case NORTH:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 0.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//north
                        break;
                    case SOUTH:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 16.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//south
                        break;
                    case EAST:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(16.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//east
                        break;
                    case WEST:
                        list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(0.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(side), side, modelRot, null, scale, true));//west
                        break;
                }
            } else { //This appears to be the same as getGeneralQuads
                    list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 0.0F, 16.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.DOWN), EnumFacing.DOWN, modelRot, null, scale, true));//down
                    list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 16.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.UP), EnumFacing.UP, modelRot, null, scale, true));//up
                    list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 0.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.NORTH), EnumFacing.NORTH, modelRot, null, scale, true));//north
                    list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 16.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.SOUTH), EnumFacing.SOUTH, modelRot, null, scale, true));//south
                    list.add(faceBakery.makeBakedQuad(new Vector3f(16.0F, 0.0F, 0.0F), new Vector3f(16.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.EAST), EnumFacing.EAST, modelRot, null, scale, true));//east
                    list.add(faceBakery.makeBakedQuad(new Vector3f(0.0F, 0.0F, 0.0F), new Vector3f(0.0F, 16.0F, 16.0F), face, textureAtlasSpriteHashMap.get(EnumFacing.WEST), EnumFacing.WEST, modelRot, null, scale, true));//west
            }
            return list;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }


        @Override
        public boolean isAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isBuiltInRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleTexture() {
            return textureAtlasSpriteHashMap.get(EnumFacing.DOWN);
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return ItemCameraTransforms.DEFAULT;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }


        //TODO rewrite this so its not a using old ways
        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {

            if (cameraTransformType == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND || cameraTransformType == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND)
                return Pair.of(IBakedModel.class.cast(this), ForgeHooksClient.getMatrix(new ItemTransformVec3f(new Vector3f(3.3F, 1, -0.3F), new Vector3f(0F, 0.1F, -0.15F), new Vector3f(0.35F, 0.35F, 0.35F))));

            if (cameraTransformType == ItemCameraTransforms.TransformType.GUI)
                return Pair.of(IBakedModel.class.cast(this), ForgeHooksClient.getMatrix(new ItemTransformVec3f(new Vector3f(3.45F, 0.7F, 2.8F), new Vector3f(0F, 0.0F, 0.0F), new Vector3f(0.6F, 0.6F, 0.6F))));

            if (cameraTransformType == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND || cameraTransformType == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND)
                return Pair.of(IBakedModel.class.cast(this), ForgeHooksClient.getMatrix(new ItemTransformVec3f(new Vector3f(0F, 0, 0F), new Vector3f(0F, 0.0F, 0F), new Vector3f(0.35F, 0.35F, 0.35F))));

            return Pair.of(IBakedModel.class.cast(this), null);
        }

    }

    //Item
    public class ItemIconInfo {

        Item item;
        int damage;
        TextureAtlasSprite sprite;
        String textureName;

        public boolean isBucket = false;

        public Item getItem() {
            return item;
        }

        public TextureAtlasSprite getSprite() {
            return sprite;
        }

        public ItemIconInfo(Item item, int damage, TextureAtlasSprite sprite, String textureName) {
            this.item = item;
            this.damage = damage;
            this.sprite = sprite;
            this.textureName = textureName;
        }

    }


    public class CustomBakedModel implements IPerspectiveAwareModel {
        private final ImmutableList<BakedQuad> quads;
        private final TextureAtlasSprite particle;
        private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms;
        private final IBakedModel otherModel;
        private final boolean isCulled;

        public CustomBakedModel(ImmutableList<BakedQuad> quads, TextureAtlasSprite particle, ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> transforms, IBakedModel otherModel)
        {
            this.quads = quads;
            this.particle = particle;
            this.transforms = transforms;
            if(otherModel != null)
            {
                this.otherModel = otherModel;
                this.isCulled = true;
            }
            else
            {
                ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
                for(BakedQuad quad : quads)
                {
                    if(quad.getFace() == EnumFacing.SOUTH)
                    {
                        builder.add(quad);
                    }
                }
                this.otherModel = new CustomBakedModel(builder.build(), particle, transforms, this);
                isCulled = false;
            }
        }

        public boolean isAmbientOcclusion() { return true; }
        public boolean isGui3d() { return false; }
        public boolean isBuiltInRenderer() { return false; }
        public TextureAtlasSprite getParticleTexture() { return particle; }
        public ItemCameraTransforms getItemCameraTransforms() { return ItemCameraTransforms.DEFAULT; }
        public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }

        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand)
        {
            if(side == null) return quads;
            return ImmutableList.of();
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
//            Pair<? extends IBakedModel, Matrix4f> pair = IPerspectiveAwareModel.MapWrapper.handlePerspective(this, transforms, type);
//            if(type == ItemCameraTransforms.TransformType.GUI && !isCulled && pair.getRight() == null)
//            {
//                return Pair.of(otherModel, null);
//            }
//            else if(type != ItemCameraTransforms.TransformType.GUI && isCulled)
//            {
//                return Pair.of(otherModel, pair.getRight());
//            }
            return Pair.of(this, ForgeHooksClient.getMatrix(ItemCameraTransforms.DEFAULT.getTransform(type)));
        }
    }


}
