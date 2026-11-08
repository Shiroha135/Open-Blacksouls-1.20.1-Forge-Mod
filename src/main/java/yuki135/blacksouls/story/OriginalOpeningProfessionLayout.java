package yuki135.blacksouls.story;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import yuki135.blacksouls.handler.OriginalOpeningSourceBlocks;
import static yuki135.blacksouls.story.OriginalOpeningScenes.*;

public final class OriginalOpeningProfessionLayout {
    private static final List<Placement> PLACEMENTS=load();

    private record Placement(String module,BlockPos position,Direction facing,String category) { }

    private OriginalOpeningProfessionLayout() { }

    private static List<Placement> load() {
        try(var input=OriginalOpeningProfessionLayout.class.getResourceAsStream("/data/blacksouls/original_opening_profession_layout.json")) {
            if(input==null)throw new IllegalStateException("Missing original profession layout");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1||root.get("sourceMap").getAsInt()!=7)throw new IllegalArgumentException("Invalid original profession layout source");
            var size=root.getAsJsonArray("size");
            if(size.size()!=2||size.get(0).getAsInt()!=20||size.get(1).getAsInt()!=20)throw new IllegalArgumentException("Invalid original profession layout bounds");
            var result=new ArrayList<Placement>();
            for(var entry:root.getAsJsonArray("placements")) {
                var row=entry.getAsJsonObject();var position=row.getAsJsonArray("position");
                var facing=Direction.byName(row.get("facing").getAsString());
                if(position.size()!=3||facing==null||facing.getAxis()==Direction.Axis.Y)throw new IllegalArgumentException("Invalid original profession placement");
                var point=new BlockPos(position.get(0).getAsInt(),position.get(1).getAsInt(),position.get(2).getAsInt());
                String category=row.get("category").getAsString();
                if(!Set.of("tree","structure","boat","plant").contains(category))throw new IllegalArgumentException("Invalid original profession placement category");
                result.add(new Placement(row.get("module").getAsString(),point,facing,category));
            }
            if(result.size()<4||result.size()>128)throw new IllegalArgumentException("Invalid original profession placement count");
            return List.copyOf(result);
        } catch(java.io.IOException failure) { throw new IllegalStateException("Cannot read original profession layout",failure); }
    }

    public static OriginalOpeningProfessionStage.Template template() {
        var blocks=new LinkedHashMap<BlockPos,Material>();
        var floor=new LinkedHashMap<BlockPos,Material>();
        OriginalOpeningSourceBlocks.append(floor,"source_ground",BlockPos.ZERO,Direction.NORTH);
        var material=floor.get(BlockPos.ZERO);
        if(material==null||!OriginalOpeningSourceBlocks.MODULES.get("source_ground").parts().get(0).solid())throw new IllegalStateException("Original profession floor requires a solid source block");
        for(int z=0;z<20;z++)for(int x=0;x<20;x++) {
            blocks.put(new BlockPos(x,-1,z),material);blocks.put(new BlockPos(x,-2,z),material);
        }
        var treeBlocks=new java.util.HashSet<Block>();
        var solidTreeBlocks=new java.util.HashSet<Block>();
        OriginalOpeningSourceBlocks.MODULES.get("source_tree").parts().forEach(part->treeBlocks.add(part.block().get()));
        OriginalOpeningSourceBlocks.MODULES.get("source_tree").parts().stream().filter(OriginalOpeningSourceBlocks.Part::solid).forEach(part->solidTreeBlocks.add(part.block().get()));
        for(var placement:PLACEMENTS) {
            var addition=new LinkedHashMap<BlockPos,Material>();
            OriginalOpeningSourceBlocks.append(addition,placement.module(),placement.position(),placement.facing());
            if(placement.category().equals("plant")&&addition.keySet().stream().anyMatch(blocks::containsKey))continue;
            for(var entry:addition.entrySet()) {
                var position=entry.getKey();
                if(position.getX()<0||position.getX()>=20||position.getZ()<0||position.getZ()>=20
                        ||position.getY()<MIN_Y-FEET_Y||position.getY()>=MAX_Y-FEET_Y)throw new IllegalStateException("Original profession placement escaped bounds: "+position);
                var previous=blocks.get(position);
                if(previous==null)blocks.put(position,entry.getValue());
                else if(placement.category().equals("boat")&&treeBlocks.contains(previous.state().getBlock())&&!solidTreeBlocks.contains(previous.state().getBlock()))blocks.put(position,entry.getValue());
                else if(!placement.category().equals("plant"))throw new IllegalStateException("Original profession structure overlap: "+position);
            }
        }
        var result=new OriginalOpeningProfessionStage.Template(blocks);
        var lease=new OriginalOpeningSceneData.Lease(new java.util.UUID(0,0),0,result);
        var plan=OriginalOpeningProfessionStage.plan(lease,result);
        checkpoint(plan,new Cell(10,9));
        professionAnchors(lease).forEach(anchor->checkpoint(plan,anchor.sourceCell()));
        return result;
    }
}
