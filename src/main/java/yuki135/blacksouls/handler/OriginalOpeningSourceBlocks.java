package yuki135.blacksouls.handler;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import yuki135.blacksouls.BlackSouls;
import yuki135.blacksouls.story.OriginalOpeningProfessionStage;
import yuki135.blacksouls.story.OriginalOpeningScenes.Material;

public final class OriginalOpeningSourceBlocks {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,BlackSouls.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,BlackSouls.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,BlackSouls.MODID);
    private static final List<RegistryObject<? extends Item>> MENU_ITEMS=new ArrayList<>();
    public static final Map<String,Module> MODULES=load();
    private static final RegistryObject<CreativeModeTab> TAB=TABS.register("opening_source_materials",()->CreativeModeTab.builder()
            .title(Component.literal("序章·原作图块")).icon(()->new ItemStack(MENU_ITEMS.get(0).get()))
            .displayItems((parameters,output)->MENU_ITEMS.forEach(item->output.accept(item.get()))).build());

    public record Part(BlockPos offset,boolean solid,RegistryObject<Block> block) { }
    public record Module(String name,String title,List<Part> parts) { }

    private OriginalOpeningSourceBlocks() { }

    public static void register(IEventBus bus) { BLOCKS.register(bus);ITEMS.register(bus);TABS.register(bus); }

    private static Map<String,Module> load() {
        try(var input=OriginalOpeningSourceBlocks.class.getResourceAsStream("/data/blacksouls/original_opening_source_blocks.json")) {
            if(input==null)throw new IllegalStateException("Missing original opening source blocks");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var modules=new LinkedHashMap<String,Module>();
            for(var value:root.getAsJsonArray("modules")) {
                var row=value.getAsJsonObject();String name=row.get("name").getAsString(),title=row.get("title").getAsString();
                if(!name.matches("source_[a-z0-9_]+"))throw new IllegalArgumentException("Invalid source block module");
                var parts=new ArrayList<Part>();
                for(var entry:row.getAsJsonArray("parts")) {
                    var part=entry.getAsJsonObject();var offset=part.getAsJsonArray("offset");boolean solid=part.get("solid").getAsBoolean();
                    String id="opening_"+name+"_part_"+parts.size();
                    var collision=collision(part,solid);
                    parts.add(new Part(new BlockPos(offset.get(0).getAsInt(),offset.get(1).getAsInt(),offset.get(2).getAsInt()),solid,
                            BLOCKS.register(id,()->new SourceBlock(collision))));
                }
                var module=new Module(name,title,List.copyOf(parts));
                if(parts.isEmpty()||parts.size()>128||modules.put(name,module)!=null)throw new IllegalArgumentException("Invalid source block module parts");
                for(int index=0;index<parts.size();index++) {
                    int part=index;
                    var item=ITEMS.register("opening_"+name+"_part_"+part,()->part==0?new PieceItem(module):new PartItem(module,part));
                    if(part==0)MENU_ITEMS.add(item);
                }
                ITEMS.register("opening_"+name,()->new LegacyPieceItem(module));
            }
            return Collections.unmodifiableMap(modules);
        } catch(java.io.IOException failure) { throw new IllegalStateException("Cannot load original source blocks",failure); }
    }

    private static List<double[]> collision(JsonObject part,boolean solid) {
        if(!part.has("collision"))return solid?List.of(new double[]{0,0,0,16,16,16}):List.of();
        var boxes=new ArrayList<double[]>();
        var rows=part.getAsJsonArray("collision");
        if(rows.size()>128)throw new IllegalArgumentException("Source block collision exceeds limit");
        for(var row:rows) {
            var values=row.getAsJsonArray();
            if(values.size()!=6)throw new IllegalArgumentException("Invalid source block collision box");
            double[] box=new double[6];
            for(int index=0;index<6;index++) {
                box[index]=values.get(index).getAsDouble();
                if(!Double.isFinite(box[index])||box[index]<0||box[index]>16)throw new IllegalArgumentException("Source block collision escaped its cell");
            }
            if(box[0]>=box[3]||box[1]>=box[4]||box[2]>=box[5])throw new IllegalArgumentException("Empty source block collision box");
            boxes.add(box);
        }
        if(solid==boxes.isEmpty())throw new IllegalArgumentException("Source block collision disagrees with material");
        return List.copyOf(boxes);
    }

    public static void append(Map<BlockPos,Material> blocks,String name,BlockPos origin,Direction facing) {
        var module=MODULES.get(name);
        if(module==null)throw new IllegalArgumentException("未知的原作图块部件："+name);
        for(var part:module.parts()) {
            var offset=part.offset();
            var rotated=switch(facing) {
                case NORTH->offset;
                case EAST->new BlockPos(-offset.getZ(),offset.getY(),offset.getX());
                case SOUTH->new BlockPos(-offset.getX(),offset.getY(),-offset.getZ());
                case WEST->new BlockPos(offset.getZ(),offset.getY(),-offset.getX());
                default->throw new IllegalArgumentException("图块只能水平旋转。");
            };
            var material=Material.of(part.block().get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing),part.solid());
            if(blocks.putIfAbsent(origin.offset(rotated),material)!=null)throw new IllegalArgumentException("原作图块部件重叠。");
        }
    }

    private static final class SourceBlock extends HorizontalDirectionalBlock {
        private final VoxelShape[] shapes=new VoxelShape[4];
        private SourceBlock(List<double[]> boxes) {
            super(BlockBehaviour.Properties.of().strength(.5F).noOcclusion());
            for(var facing:Direction.Plane.HORIZONTAL) {
                VoxelShape shape=Shapes.empty();
                for(var box:boxes) {
                    var rotated=switch(facing) {
                        case EAST->new double[]{16-box[5],box[1],box[0],16-box[2],box[4],box[3]};
                        case SOUTH->new double[]{16-box[3],box[1],16-box[5],16-box[0],box[4],16-box[2]};
                        case WEST->new double[]{box[2],box[1],16-box[3],box[5],box[4],16-box[0]};
                        default->box;
                    };
                    shape=Shapes.or(shape,Block.box(rotated[0],rotated[1],rotated[2],rotated[3],rotated[4],rotated[5]));
                }
                shapes[facing.get2DDataValue()]=shape.optimize();
            }
            registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING); }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()); }
        @Override public BlockState rotate(BlockState state,Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
        @Override public BlockState mirror(BlockState state,Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
        @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos position,CollisionContext context) { return shapes[state.getValue(FACING).get2DDataValue()]; }
        @Override public float getShadeBrightness(BlockState state,BlockGetter level,BlockPos position) { return 1F; }
        @Override public boolean propagatesSkylightDown(BlockState state,BlockGetter level,BlockPos position) { return true; }
    }

    private static final class PieceItem extends BlockItem {
        private final Module module;
        private PieceItem(Module module) { super(module.parts().get(0).block().get(),new Item.Properties());this.module=module; }
        @Override public Component getName(ItemStack stack) { return Component.literal(module.title()); }
        @Override public InteractionResult useOn(UseOnContext context) {
            var player=context.getPlayer();
            if(player==null||!player.mayBuild()||yuki135.blacksouls.story.OriginalOpeningScenes.DIMENSION.equals(context.getLevel().dimension()))return InteractionResult.FAIL;
            if(module.parts().size()==1)return super.useOn(context);
            if(context.getLevel().isClientSide())return InteractionResult.SUCCESS;
            try {
                OriginalOpeningProfessionStage.piece((ServerLevel)context.getLevel(),module.name(),context.getClickedPos().relative(context.getClickedFace()),player.getDirection().getOpposite());
                if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
                return InteractionResult.CONSUME;
            } catch(IllegalArgumentException|IllegalStateException failure) {
                player.displayClientMessage(Component.literal(failure.getMessage()),true);return InteractionResult.FAIL;
            }
        }
    }

    private static final class PartItem extends BlockItem {
        private final Module module;
        private final int part;
        private PartItem(Module module,int part) { super(module.parts().get(part).block().get(),new Item.Properties());this.module=module;this.part=part; }
        @Override public Component getName(ItemStack stack) { return Component.literal(module.title()+"·部件 "+(part+1)+"/"+module.parts().size()); }
        @Override public InteractionResult useOn(UseOnContext context) {
            if(yuki135.blacksouls.story.OriginalOpeningScenes.DIMENSION.equals(context.getLevel().dimension()))return InteractionResult.FAIL;
            return super.useOn(context);
        }
    }

    private static final class LegacyPieceItem extends Item {
        private final Module module;
        private LegacyPieceItem(Module module) { super(new Item.Properties());this.module=module; }
        @Override public Component getName(ItemStack stack) { return Component.literal(module.title()); }
        @Override public void inventoryTick(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity entity,int slot,boolean selected) {
            if(level.isClientSide()||!(entity instanceof net.minecraft.world.entity.player.Player player))return;
            var inventory=player.getInventory();
            for(int index=0;index<inventory.getContainerSize();index++)if(inventory.getItem(index)==stack) {
                var replacement=new ItemStack(module.parts().get(0).block().get(),stack.getCount());
                if(stack.hasTag())replacement.setTag(stack.getTag().copy());
                inventory.setItem(index,replacement);inventory.setChanged();return;
            }
        }
    }
}
