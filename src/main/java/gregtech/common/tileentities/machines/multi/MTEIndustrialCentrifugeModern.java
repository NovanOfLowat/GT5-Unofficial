package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.InputHatch;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.Muffler;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.HatchElement.OutputHatch;
import static gregtech.api.enums.Textures.BlockIcons.LARGETURBINE_NEW5;
import static gregtech.api.enums.Textures.BlockIcons.LARGETURBINE_NEW_ACTIVE5;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.chainAllGlasses;
import static gregtech.api.util.GTStructureUtility.ofFrame;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.casing.Casings;
import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.INEIPreviewModifier;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.render.RenderOverlay;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTUtilityClient;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.pollution.PollutionConfig;
import gtPlusPlus.api.recipe.GTPPRecipeMaps;

public class MTEIndustrialCentrifugeModern extends MTEExtendedPowerMultiBlockBase<MTEIndustrialCentrifugeModern>
    implements ISurvivalConstructable, INEIPreviewModifier {

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final IStructureDefinition<MTEIndustrialCentrifugeModern> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEIndustrialCentrifugeModern>builder()
        .addShape(
            STRUCTURE_PIECE_MAIN,
            // spotless:off
            new String[][]{{
                "BBB",
                "BBB",
                "B~B",
                "BBB",
                "C C"
            },{
                "BBB",
                "A A",
                "A A",
                "BBB",
                "   "
            },{
                "BBB",
                "BAB",
                "BAB",
                "BBB",
                "C C"
            }})
        //spotless:on
        .addElement(
            'B',
            buildHatchAdder(MTEIndustrialCentrifugeModern.class)
                .atLeast(InputBus, OutputBus, InputHatch, OutputHatch, Maintenance, Energy, Muffler)
                .casingIndex(Casings.CentrifugeCasing.textureId)
                .dot(1)
                .buildAndChain(
                    onElementPass(MTEIndustrialCentrifugeModern::onCasingAdded, Casings.CentrifugeCasing.asElement())))
        .addElement('A', chainAllGlasses())
        .addElement('C', ofFrame(Materials.Steel))
        .build();

    private int mCasingAmount;
    private boolean mIsAnimated = true;
    private final List<RenderOverlay.OverlayTicket> overlayTickets = new ArrayList<>();
    private boolean mFormed;

    public MTEIndustrialCentrifugeModern(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEIndustrialCentrifugeModern(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<MTEIndustrialCentrifugeModern> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEIndustrialCentrifugeModern(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        ITexture[] rTexture;
        if (side == aFacing) {
            if (aActive && mIsAnimated) {
                rTexture = new ITexture[] { Casings.CentrifugeCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(LARGETURBINE_NEW_ACTIVE5)
                    .extFacing()
                    .build() };
            } else {
                rTexture = new ITexture[] { Casings.CentrifugeCasing.getCasingTexture(), TextureFactory.builder()
                    .addIcon(LARGETURBINE_NEW5)
                    .extFacing()
                    .build() };
            }
        } else {
            rTexture = new ITexture[] { Casings.CentrifugeCasing.getCasingTexture() };
        }
        return rTexture;
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Centrifuge")
            .addInfo("125% faster than single block machines of the same voltage")
            .addInfo("Disable animations with a screwdriver")
            .addInfo("Only uses 90% of the EU/t normally required")
            .addInfo("Processes six items per voltage tier")
            .addPollutionAmount(PollutionConfig.pollutionPerSecondMultiIndustrialCentrifuge)
            .beginStructureBlock(3, 5, 3, true)
            .addController("Front Center")
            .addCasingInfoMin("Reinforced Wooden Casing", 14, false)
            .addCasingInfoExactly("Any Tiered Glass", 6, false)
            .addCasingInfoExactly("Steel Frame Box", 4, false)
            .addInputBus("Any Wooden Casing", 1)
            .addOutputBus("Any Wooden Casing", 1)
            .addInputHatch("Any Wooden Casing", 1)
            .addOutputHatch("Any Wooden Casing", 1)
            .addEnergyHatch("Any Wooden Casing", 1)
            .addMaintenanceHatch("Any Wooden Casing", 1)
            .addMufflerHatch("Any Wooden Casing", 1)
            .addSubChannelUsage(GTStructureChannels.BOROGLASS)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 1, 2, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 1, 2, 0, elementBudget, env, false, true);
    }

    private void onCasingAdded() {
        mCasingAmount++;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasingAmount = 0;
        checkPiece(STRUCTURE_PIECE_MAIN, 1, 2, 0);
        if (mMaintenanceHatches.isEmpty()) return false;
        if (mCasingAmount < 14) return false;
        return true;
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic().noRecipeCaching()
            .setEuModifier(0.9F)
            .setSpeedBonus(1F / 2.25F)
            .setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public int getMaxParallelRecipes() {
        return (6 * GTUtility.getTier(this.getMaxInputVoltage()));
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return GTPPRecipeMaps.centrifugeNonCellRecipes;
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
    public int getPollutionPerSecond(ItemStack aStack) {
        return PollutionConfig.pollutionPerSecondMultiIndustrialCentrifuge;
    }

    public boolean usingAnimations() {
        return mIsAnimated;
    }

    @Override
    public final void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        super.onScrewdriverRightClick(side, aPlayer, aX, aY, aZ, aTool);
        this.mIsAnimated = !mIsAnimated;
        if (this.mIsAnimated) {
            GTUtility.sendChatToPlayer(aPlayer, "Using Animated Turbine Texture. ");
        } else {
            GTUtility.sendChatToPlayer(aPlayer, "Using Static Turbine Texture. ");
        }
        setTurbineOverlay();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setBoolean("mIsAnimated", mIsAnimated);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aNBT.hasKey("mIsAnimated")) {
            mIsAnimated = aNBT.getBoolean("mIsAnimated");
        }
    }

    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        super.onFirstTick(aBaseMetaTileEntity);
        setTurbineOverlay();
    }

    @Override
    public void setExtendedFacing(ExtendedFacing newExtendedFacing) {
        boolean extendedFacingChanged = newExtendedFacing != getExtendedFacing();
        super.setExtendedFacing(newExtendedFacing);
        if (extendedFacingChanged) {
            setTurbineOverlay();
        }
    }

    @Override
    public void onTextureUpdate() {
        setTurbineOverlay();
    }

    protected void setTurbineOverlay() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile.isServerSide()) return;

        IIconContainer[] tTextures;
        if (getBaseMetaTileEntity().isActive() && usingAnimations()) tTextures = Textures.BlockIcons.TURBINE_NEW_ACTIVE;
        else tTextures = Textures.BlockIcons.TURBINE_NEW;

        GTUtilityClient.setTurbineOverlay(
            tile.getWorld(),
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord(),
            getExtendedFacing(),
            tTextures,
            overlayTickets);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        if (getBaseMetaTileEntity().isClientSide()) GTUtilityClient.clearTurbineOverlay(overlayTickets);
    }

    @Override
    public void onValueUpdate(byte aValue) {
        mFormed = (aValue & 0x1) != 0;
        setTurbineOverlay();
    }

    @Override
    public byte getUpdateData() {
        return (byte) ((mMachine ? 1 : 0));
    }

    @Override
    public void onPreviewStructureComplete(@NotNull ItemStack trigger) {
        mFormed = true;
    }
}
