package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.*;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;
import gregtech.api.casing.Casings;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures.BlockIcons;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;

public class MTEPyrolyseOvenModern extends MTEEnhancedMultiBlockBase<MTEPyrolyseOvenModern>
    implements ISurvivalConstructable {

    private static final int HORIZONTAL_OFFSET = 2;
    private static final int VERTICAL_OFFSET = 3;
    private static final int DEPTH_OFFSET = 0;
    private static final String STRUCTURE_PIECE_MAIN = "main";

    private HeatingCoilLevel coilHeat;
    private int mCasingAmount;

    private static final IStructureDefinition<MTEPyrolyseOvenModern> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEPyrolyseOvenModern>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            transpose(
                new String[][] { { "ccccc", "ctttc", "ctttc", "ctttc", "ccccc" },
                    { "ccccc", "c---c", "c---c", "c---c", "ccccc" }, { "ccccc", "c---c", "c---c", "c---c", "ccccc" },
                    { "bb~bb", "bCCCb", "bCCCb", "bCCCb", "bbbbb" }, }))
        .addElement('c', onElementPass(MTEPyrolyseOvenModern::onCasingAdded, Casings.PyrolyseCasing.asElement()))
        .addElement(
            'C',
            gregtech.api.util.GTStructureUtility.activeCoils(
                gregtech.api.util.GTStructureUtility
                    .ofCoil(MTEPyrolyseOvenModern::setCoilLevel, MTEPyrolyseOvenModern::getCoilLevel)))
        .addElement(
            'b',
            gregtech.api.util.GTStructureUtility.buildHatchAdder(MTEPyrolyseOvenModern.class)
                .atLeast(OutputBus, OutputHatch, Energy, Maintenance)
                .casingIndex(Casings.PyrolyseCasing.textureId)
                .dot(1)
                .buildAndChain(onElementPass(MTEPyrolyseOvenModern::onCasingAdded, Casings.PyrolyseCasing.asElement())))
        .addElement(
            't',
            gregtech.api.util.GTStructureUtility.buildHatchAdder(MTEPyrolyseOvenModern.class)
                .atLeast(InputBus, InputHatch, Muffler)
                .casingIndex(Casings.PyrolyseCasing.textureId)
                .dot(2)
                .buildAndChain(onElementPass(MTEPyrolyseOvenModern::onCasingAdded, Casings.PyrolyseCasing.asElement())))
        .build();

    public MTEPyrolyseOvenModern(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEPyrolyseOvenModern(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEPyrolyseOvenModern(mName);
    }

    @Override
    public IStructureDefinition<MTEPyrolyseOvenModern> getStructureDefinition() {
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

    private void onCasingAdded() {
        mCasingAmount++;
    }

    public HeatingCoilLevel getCoilLevel() {
        return coilHeat;
    }

    private void setCoilLevel(HeatingCoilLevel aCoilLevel) {
        coilHeat = aCoilLevel;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        coilHeat = HeatingCoilLevel.None;
        mCasingAmount = 0;
        if (checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET)) return false;
        if (mMaintenanceHatches.size() == 1) return false;
        if (!mMufflerHatches.isEmpty()) return false;
        return mCasingAmount >= 60;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection sideDirection,
        ForgeDirection facingDirection, int colorIndex, boolean active, boolean redstoneLevel) {
        if (sideDirection == facingDirection) {
            if (active) return new ITexture[] { Casings.PyrolyseCasing.getCasingTexture(), TextureFactory.builder()
                .addIcon(BlockIcons.OVERLAY_FRONT_PYROLYSE_OVEN_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(BlockIcons.OVERLAY_FRONT_PYROLYSE_OVEN_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Casings.PyrolyseCasing.getCasingTexture(), TextureFactory.builder()
                .addIcon(BlockIcons.OVERLAY_FRONT_PYROLYSE_OVEN)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(BlockIcons.OVERLAY_FRONT_PYROLYSE_OVEN_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Casings.PyrolyseCasing.getCasingTexture() };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        return new MultiblockTooltipBuilder().addMachineType("Coke Oven")
            .addInfo("Placeholder for tooltip")
            .toolTipFinisher();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.pyrolyseRecipes;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            public CheckRecipeResult process() {
                setSpeedBonus(2f / (1 + coilHeat.getTier()));
                return super.process();
            }
        };
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
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
    public boolean onWireCutterRightClick(ForgeDirection side, ForgeDirection wrenchingSide, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool) {
        if (aPlayer.isSneaking()) {
            batchMode = !batchMode;
            GTUtility.sendChatToPlayer(
                aPlayer,
                StatCollector.translateToLocal(batchMode ? "misc.BatchModeTextOn" : "misc.BatchModeTextOff"));
            return true;
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_FIRE;
    }

    @Override
    public int getPollutionPerSecond(ItemStack aStack) {
        return GTMod.proxy.mPollutionPyrolyseOvenPerSecond;
    }
}
