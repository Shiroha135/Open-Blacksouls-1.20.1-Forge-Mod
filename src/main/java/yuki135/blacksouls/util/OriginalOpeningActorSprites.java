package yuki135.blacksouls.util;

import yuki135.blacksouls.story.OriginalOpeningSceneData.Lease;
import yuki135.blacksouls.story.OriginalOpeningScenes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class OriginalOpeningActorSprites {
    private static final List<Sprite> SPRITES=load();

    public record Sprite(String sourceCharacter,ResourceLocation texture,int sheetWidth,int sheetHeight,int columns,int rows,int column,int row) {
        public int frameWidth() { return sheetWidth/columns; }
        public int frameHeight() { return sheetHeight/rows; }
        public float u0() { return column/(float)columns; }
        public float u1() { return (column+1)/(float)columns; }
        public float v0() { return row/(float)rows; }
        public float v1() { return (row+1)/(float)rows; }
    }

    private OriginalOpeningActorSprites() { }

    public static Sprite get(int origin) {
        if (origin<0 || origin>=SPRITES.size()) throw new IllegalArgumentException("Invalid opening sprite origin");
        return SPRITES.get(origin);
    }

    private static List<Sprite> load() {
        var result=new ArrayList<Sprite>();result.add(sprite("$アリス",0,4,1));
        for (var anchor:OriginalOpeningScenes.professionAnchors(new Lease(new UUID(0,0),0))) {
            var graphic=anchor.graphic();result.add(sprite(graphic.character(),graphic.index(),graphic.direction(),graphic.pattern()));
        }
        if (result.size()!=10) throw new IllegalStateException("Opening actor source sprite catalog is incomplete");return List.copyOf(result);
    }

    private static Sprite sprite(String name,int character,int direction,int pattern) {
        var entry=OriginalNpcTextures.character(name);var texture=OriginalNpcTextures.texture(entry);
        if (entry==null || texture==null) throw new IllegalStateException("Missing opening actor source sprite: "+name);
        int columns=entry.get("columns").getAsInt(),rows=entry.get("rows").getAsInt(),width=entry.get("width").getAsInt(),height=entry.get("height").getAsInt();
        boolean single=entry.get("single").getAsBoolean();int column=(single?0:character%4)*3+pattern,row=(single?0:character/4)*4+switch(direction){case 2->0;case 4->1;case 6->2;case 8->3;default->throw new IllegalArgumentException("Invalid source sprite direction");};
        if (columns!=(single?3:12) || rows!=(single?4:8) || width<=0 || height<=0 || width%columns!=0 || height%rows!=0
                || character<0 || character>(single?0:7) || pattern<0 || pattern>2 || column<0 || column>=columns || row<0 || row>=rows) throw new IllegalStateException("Invalid opening actor source sprite metadata: "+name);
        return new Sprite(name,texture,width,height,columns,rows,column,row);
    }
}
