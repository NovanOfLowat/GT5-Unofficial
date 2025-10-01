package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Mods.EnderIO;
import static gregtech.api.enums.Mods.Railcraft;
import static gregtech.api.enums.Mods.ThaumicBases;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.ITierConverter;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.casing.Casings;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

public class MTEIndustrialForgeHammerModern extends MTEExtendedPowerMultiBlockBase<MTEIndustrialForgeHammerModern>
    implements ISurvivalConstructable {

    private static final int HORIZONTAL_OFFSET = 1;
    private static final int VERTICAL_OFFSET = 1;
    private static final int DEPTH_OFFSET = 0;
    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<MTEIndustrialForgeHammerModern> STRUCTURE_DEFINITION;

    private int mCasingAmount;
    private int mAnvilTier = 0;

    static {
        Map<Block, Integer> anvilTiers = new HashMap<>();
        anvilTiers.put(Blocks.anvil, 1);
        if (Railcraft.isModLoaded()) {
            anvilTiers.put(GameRegistry.findBlock(Railcraft.ID, "anvil"), 2);
        }
        if (EnderIO.isModLoaded()) {
            anvilTiers.put(GameRegistry.findBlock(EnderIO.ID, "blockDarkSteelAnvil"), 3);
        }
        if (ThaumicBases.isModLoaded()) {
            anvilTiers.put(GameRegistry.findBlock(ThaumicBases.ID, "thaumicAnvil"), 3);
            anvilTiers.put(GameRegistry.findBlock(ThaumicBases.ID, "voidAnvil"), 4);
        }

        STRUCTURE_DEFINITION = StructureDefinition.<MTEIndustrialForgeHammerModern>builder()
            .addShape(
                STRUCTURE_PIECE_MAIN,
                transpose(new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "CAC", "CCC" }, { "CCC", "CCC", "CCC" } }))
            .addElement(
                'C',
                buildHatchAdder(MTEIndustrialForgeHammerModern.class)
                    .atLeast(InputBus, OutputBus, Maintenance, Energy, Muffler, InputHatch, OutputHatch)
                    .casingIndex(Casings.IndustrialForgeHammerCasing.textureId)
                    .dot(1)
                    .buildAndChain(
                        onElementPass(
                            MTEIndustrialForgeHammerModern::onCasingAdded,
                            Casings.IndustrialForgeHammerCasing.asElement())))
            .addElement(
                'A',
                ofBlocksTiered(
                    anvilTierConverter(anvilTiers),
                    getAllAnvilTiers(anvilTiers),
                    0,
                    MTEIndustrialForgeHammerModern::setAnvilTier,
                    MTEIndustrialForgeHammerModern::getAnvilTier))
            .build();
    }

    public MTEIndustrialForgeHammerModern(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEIndustrialForgeHammerModern(final String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(final IGregTechTileEntity aTileEntity) {
        return new MTEIndustrialForgeHammerModern(this.mName);
    }

    @Override
    public IStructureDefinition<MTEIndustrialForgeHammerModern> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET)) return false;
        if (mMaintenanceHatches.isEmpty()) return false;
        if (mCasingAmount < 6) return false;
        return true;
    }

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean aActive, boolean aRedstone) {
        if (side == facing) {
            if (aActive) return new ITexture[] { Casings.IndustrialForgeHammerCasing.getCasingTexture(),
                TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCAIndustrialForgeHammerActive)
                    .extFacing()
                    .build(),
                TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCAIndustrialForgeHammerActiveGlow)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Casings.IndustrialForgeHammerCasing.getCasingTexture(), TextureFactory.builder()
                .addIcon(TexturesGtBlock.oMCAIndustrialForgeHammer)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(TexturesGtBlock.oMCAIndustrialForgeHammerGlow)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Casings.IndustrialForgeHammerCasing.getCasingTexture() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("placeholder")
            .addInfo("Placeholder for a tooltip")
            .toolTipFinisher();
    }

    @Override
    protected SoundResource getProcessStartSound() {
        return SoundResource.GTCEU_LOOP_FORGE_HAMMER;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.hammerRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().noRecipeCaching()
            .setSpeedBonus(1 / 2F)
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public int getMaxParallelRecipes() {
        return (8 * getAnvilTier() * GTUtility.getTier(this.getMaxInputVoltage()));
    }

    private void setAnvilTier(int tier) {
        mAnvilTier = tier;
    }

    private int getAnvilTier() {
        return mAnvilTier;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("tier", mAnvilTier);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        currentTip.add(
            StatCollector.translateToLocal("GT5U.machines.tier") + ": "
                + EnumChatFormatting.YELLOW
                + GTUtility.formatNumbers(tag.getInteger("tier"))
                + EnumChatFormatting.RESET);
    }

    private static List<Pair<Block, Integer>> getAllAnvilTiers(Map<Block, Integer> anvilTiers) {
        return anvilTiers.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(Map.Entry<Block, Integer>::getValue))
            .map(e -> Pair.of(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }

    private static ITierConverter<Integer> anvilTierConverter(Map<Block, Integer> anvilTiers) {
        return (block, meta) -> block == null ? null : anvilTiers.getOrDefault(block, null);
    }
}
