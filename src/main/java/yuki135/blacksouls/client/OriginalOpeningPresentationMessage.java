package yuki135.blacksouls.client;

import yuki135.blacksouls.story.OriginalOpeningPresentationData;
import java.util.ArrayList;
import java.util.List;

public final class OriginalOpeningPresentationMessage {
    public record Glyph(String text,int color,int size,int frame,int line) { }
    public record Page(List<Glyph> glyphs,int requiredFrames,int lines,boolean title) {
        public Page { glyphs=List.copyOf(glyphs); }
        public List<Glyph> visible(int sourceFrames) { return glyphs.stream().filter(glyph->glyph.frame()<=sourceFrames).toList(); }
    }

    private OriginalOpeningPresentationMessage() {
    }

    public static Page compile(List<String> source,boolean title) {
        if(source==null||source.isEmpty()||source.size()>8)throw new IllegalArgumentException("Invalid source opening page");
        var glyphs=new ArrayList<Glyph>();
        int color=OriginalOpeningPresentationData.color(0),size=OriginalOpeningPresentationData.SOURCE_FONT_SIZE,frame=0,line=0;
        for(String original:source) {
            String text=title?original.stripLeading():original;
            if(title)frame+=original.codePointCount(0,original.length()-text.length());
            for(int index=0;index<text.length();) {
                int codePoint=text.codePointAt(index);index+=Character.charCount(codePoint);
                if(codePoint=='\\'&&index<text.length()) {
                    char escape=text.charAt(index++);
                    if(escape=='c'||escape=='C') {
                        if(index>=text.length()||text.charAt(index++)!='[')throw new IllegalArgumentException("Invalid source color escape");
                        int end=text.indexOf(']',index);
                        if(end<0)throw new IllegalArgumentException("Unterminated source color escape");
                        color=OriginalOpeningPresentationData.color(Integer.parseInt(text.substring(index,end)));index=end+1;
                    } else if(escape=='{')size=Math.min(96,size+8);
                    else if(escape=='}')size=Math.max(16,size-8);
                    else if(escape=='|')frame+=60;
                    else if(escape=='.')frame+=15;
                    else if(escape=='\\')glyphs.add(new Glyph("\\",color,size,++frame,line));
                    else throw new IllegalArgumentException("Unsupported source opening escape: "+escape);
                } else glyphs.add(new Glyph(new String(Character.toChars(codePoint)),color,size,++frame,line));
            }
            line++;
        }
        return new Page(glyphs,frame,line,title);
    }
}
