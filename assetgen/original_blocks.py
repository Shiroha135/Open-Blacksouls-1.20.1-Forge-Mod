from pathlib import Path
import argparse
import hashlib
import json
import math
import re
from PIL import Image

FACES = ('north', 'south', 'east', 'west', 'up', 'down')

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def mesh(recipe):
    kind = recipe['template']
    mats = recipe['textures']
    boxes = []
    def box(x0, y0, z0, x1, y1, z1, texture='body', faces=None, collision=True):
        materials = {f: mats[texture] for f in FACES}
        for face, material in (faces or {}).items():
            materials[face] = mats[material]
        boxes.append({'bounds': [x0,y0,z0,x1,y1,z1], 'faces': materials, 'collision': collision})
    def legs(width=16, depth=16, height=11, inset=2, thickness=2):
        for x in (inset, width-inset-thickness):
            for z in (inset, depth-inset-thickness):
                box(x,0,z,x+thickness,height,z+thickness)
    def octagon(x,y,z,width,height,texture='body'):
        q=width/4
        box(x+q,y,z,x+width-q,y+height,z+width,texture)
        box(x,y,z+q,x+width,y+height,z+width-q,texture)
        box(x+q/2,y,z+q/2,x+width-q/2,y+height,z+width-q/2,texture)
    if kind == 'cube':
        box(0,0,0,16,16,16)
    elif kind == 'slab':
        box(0,0,0,16,8,16)
    elif kind == 'stairs':
        box(0,0,0,16,8,16)
        box(0,8,8,16,16,16)
    elif kind == 'rug':
        width=recipe.get('width',16)
        box(0,0,0,width,0.5,16,faces={'up':'surface'})
    elif kind in ('chair','throne'):
        legs(height=7)
        box(1,7,1,15,9,15,'seat')
        for x in (1,13): box(x,9,12,x+2,24 if kind=='throne' else 21,15)
        box(2,16,12,14,21 if kind=='chair' else 25,14,'back')
        if kind=='chair':
            for x in (5,9): box(x,9,12,x+1,16,14)
        else:
            box(3,10,13,13,24,15,'back')
            for x in (0,14):
                box(x,9,3,x+2,14,14)
                box(x,13,2,x+2,15,15)
            box(5,25,12,11,28,15)
    elif kind in ('table','long_table','stool'):
        width=32 if kind=='long_table' else 16
        height=7 if kind=='stool' else 14
        legs(width=width,height=height)
        box(0,height,0,width,height+2,16,'surface')
        if kind!='stool':
            box(2,height-2,2,width-2,height,3)
            box(2,height-2,13,width-2,height,14)
    elif kind == 'round_table':
        octagon(0,14,0,16,2,'surface')
        box(6,2,6,10,14,10)
        box(2,0,6,14,2,10)
        box(6,0,2,10,2,14)
    elif kind in ('bookcase','cupboard'):
        width=recipe.get('width',16)
        box(0,0,5,width,2,16)
        box(0,30,5,width,32,16)
        box(0,2,14,width,30,16)
        for x in (0,width-2): box(x,2,5,x+2,30,14)
        if kind=='cupboard':
            box(2,2,4,width-2,30,6,'panel')
            for x in (width/2-2,width/2+1): box(x,14,3,x+1,17,4,'trim')
        else:
            for y in (9,18,27): box(2,y,5,width-2,y+1,14)
            for row,(y,h) in enumerate(((2,7),(10,8),(19,8))):
                x=2.25
                i=0
                while x < width-3:
                    bw=min(1.3+((i+row)%3)*0.3,width-2.5-x)
                    bh=h-(i%3)*0.5
                    box(x,y,6+(i%2)*0.3,x+bw,y+bh,13,'books'+str((i+row)%4))
                    x+=bw+0.25
                    i+=1
    elif kind in ('cabinet','counter'):
        width=32 if kind=='counter' else 16
        box(0,0,1,width,14,16,'panel')
        box(0,14,0,width,16,16,'surface')
        if kind=='cabinet':
            for y in (4,9): box(5,y,0,11,y+1,1,'trim')
        else:
            box(1,1,0,width-1,3,1,'body')
    elif kind in ('bed','straw_bed'):
        legs(depth=32,height=6)
        box(0,5,0,16,7,32)
        box(1,7,1,15,10,30,'quilt')
        box(2,10,23,14,12,30,'pillow')
        box(0,7,30,16,19,32,'back')
        box(0,7,0,16,10,2)
        if kind=='straw_bed': box(1,10,2,15,11,22,'quilt')
    elif kind == 'chest':
        box(1,1,2,15,10,14)
        box(1,10,3,15,12,13,'surface')
        box(2,12,4,14,13,12,'surface')
        for x in (2,12): box(x,1,1.75,x+2,11,2.25,'trim')
        box(7,7,1,9,10,2,'trim')
    elif kind == 'barrel':
        octagon(1,1,1,14,16)
        octagon(0.5,4,0.5,15,1,'trim')
        octagon(0.5,13,0.5,15,1,'trim')
        octagon(2,17,2,12,1,'surface')
    elif kind == 'crate':
        box(0,0,0,16,16,16)
        for z in (0,15):
            for y in (0,14): box(0,y,z,16,y+2,z+1,'trim')
            for x in (0,14): box(x,2,z,x+2,14,z+1,'trim')
    elif kind in ('book_stack','closed_book','open_book'):
        if kind=='open_book':
            box(1,0,3,8,1,13,'cover')
            box(8,0,3,15,1,13,'cover')
            box(2,1,4,7,2,12,'pages')
            box(9,1,4,14,2,12,'pages')
            box(7,0.5,4,9,1.5,12,'pages')
        elif kind=='closed_book':
            box(2,0,3,14,0.5,13,'cover')
            box(3,0.5,3.5,13.5,2,12.5,'pages')
            box(2,2,3,14,2.5,13,'cover')
            box(2,0,3,3,2.5,13,'cover')
        else:
            for i,(x,z,w,d) in enumerate(((1,3,13,10),(3,1,11,13),(2,4,12,10))):
                y=i*2.5
                box(x,y,z,x+w,y+0.5,z+d,'cover')
                box(x+0.5,y+0.5,z+0.5,x+w-0.5,y+2,z+d-0.5,'pages')
                box(x,y+2,z,x+w,y+2.5,z+d,'cover')
    elif kind == 'lectern':
        box(2,0,2,14,2,14)
        box(6,2,6,10,14,10)
        for z in range(4): box(0,11+z,4*z,16,12+z,4*z+4,'surface')
        box(1,12,0,15,13,2)
        box(2,13,3,14,14,7,'pages')
        box(2,14,7,14,15,11,'pages')
    elif kind in ('candle','candelabra','wall_candle'):
        if kind=='wall_candle':
            box(5,2,14,11,10,16)
            box(7,3,7,9,5,15)
            positions=[(8,8,5)]
        elif kind=='candelabra':
            octagon(4,0,4,8,2)
            box(7,2,7,9,13,9)
            box(2,10,7,14,12,9)
            positions=[(3,8,12),(8,8,14),(13,8,12)]
        else:
            octagon(4,0,4,8,1)
            box(7,1,7,9,5,9)
            positions=[(8,8,5)]
        for x,z,y in positions:
            box(x-2,y,z-2,x+2,y+1,z+2)
            box(x-1,y+1,z-1,x+1,y+6,z+1,'wax')
            box(x-0.7,y+6,z-0.7,x+0.7,y+8,z+0.7,'flame',collision=False)
    elif kind == 'pillar':
        height=recipe.get('height',48)
        box(0,0,0,16,3,16,'trim')
        box(2,3,2,14,5,14)
        octagon(3,5,3,10,height-10)
        box(2,height-5,2,14,height-3,14)
        box(0,height-3,0,16,height,16,'trim')
    elif kind == 'arch':
        for x in (0,40):
            box(x,0,0,x+8,4,16,'trim')
            box(x+1,4,2,x+7,34,14)
            box(x,34,0,x+8,38,16,'trim')
        box(0,38,0,48,46,16)
        box(0,46,0,48,48,16,'trim')
        box(8,34,1,12,38,15)
        box(36,34,1,40,38,15)
    elif kind == 'iron_bars':
        for x in (1,5,9,13): box(x,0,7,x+1,32,9)
        for y in (2,15,28): box(0,y,7,16,y+1,9,'trim')
    elif kind == 'rubble':
        for v in [(1,0,2,8,5,9),(8,0,6,15,4,15),(4,4,5,11,8,12),(10,0,1,15,4,6),(0,0,11,5,3,16)]: box(*v)
    elif kind == 'fireplace':
        for x in (0,24): box(x,0,7,x+8,24,16)
        box(0,24,5,32,30,16,'trim')
        box(0,0,0,32,2,16,'trim')
        box(8,2,14,24,24,16,'dark')
        box(4,30,4,28,32,16,'trim')
        for x in (10,17): box(x,2,8,x+5,4,12,'log')
    elif kind == 'piano':
        for x in (1,28): box(x,0,1,x+3,11,5)
        box(0,0,7,32,24,16)
        box(0,11,1,32,13,8,'keys')
        box(0,13,7,32,16,8,'panel')
        box(0,24,6,32,26,16)
        for x in (10,20): box(x,0,3,x+1,1,7,'trim')
    elif kind == 'frame':
        width=recipe.get('width',16)
        height=recipe.get('height',16)
        box(1,1,14,width-1,height-1,15,'picture')
        for x in (0,width-1): box(x,0,13,x+1,height,16,'trim')
        for y in (0,height-1): box(1,y,13,width-1,y+1,16,'trim')
    elif kind == 'teapot':
        octagon(4,1,4,8,5,'body')
        octagon(5,6,5,6,1,'body')
        box(7,7,7,9,8,9,'trim')
        box(11,3,6,14,5,10,'body')
        box(1,2,5,3,5,11,'trim')
        box(2,5,5,5,6,11,'trim')
        box(2,1,5,5,2,11,'trim')
    elif kind == 'mug':
        octagon(5,0,5,6,5,'body')
        box(6,5,6,10,5.2,10,'liquid')
        box(11,1,7,13,4,9,'body')
    elif kind == 'plate':
        octagon(2,0,2,12,0.6,'body')
        octagon(3,0.6,3,10,0.4,'surface')
    elif kind == 'bottle':
        octagon(5,0,5,6,6,'body')
        box(6,6,6,10,8,10,'body')
        box(7,8,7,9,12,9,'neck')
        box(6.5,12,6.5,9.5,13,9.5,'trim')
    elif kind == 'paper':
        box(2,0,2,14,0.15,14,'pages')
    else:
        raise ValueError(kind)
    return boxes

def clipped(box, offset):
    v=box['bounds'];low=[offset[i]*16 for i in range(3)]
    q=[max(v[i],low[i]) for i in range(3)]+[min(v[i+3],low[i]+16) for i in range(3)]
    return q if all(q[i+3]>q[i]+1e-5 for i in range(3)) else None

def face_uv(original, sub, face):
    x0,y0,z0,x1,y1,z1=original
    a,b,c,d,e,f=sub
    if face=='north': return [16*(x1-d)/(x1-x0),16*(y1-e)/(y1-y0),16*(x1-a)/(x1-x0),16*(y1-b)/(y1-y0)]
    if face=='south': return [16*(a-x0)/(x1-x0),16*(y1-e)/(y1-y0),16*(d-x0)/(x1-x0),16*(y1-b)/(y1-y0)]
    if face=='west': return [16*(c-z0)/(z1-z0),16*(y1-e)/(y1-y0),16*(f-z0)/(z1-z0),16*(y1-b)/(y1-y0)]
    if face=='east': return [16*(z1-f)/(z1-z0),16*(y1-e)/(y1-y0),16*(z1-c)/(z1-z0),16*(y1-b)/(y1-y0)]
    if face=='up': return [16*(a-x0)/(x1-x0),16*(c-z0)/(z1-z0),16*(d-x0)/(x1-x0),16*(f-z0)/(z1-z0)]
    return [16*(a-x0)/(x1-x0),16*(z1-f)/(z1-z0),16*(d-x0)/(x1-x0),16*(z1-c)/(z1-z0)]

def model_element(box, clip, offset, inventory_scale=None):
    local=[clip[i]-offset[i%3]*16 for i in range(6)]
    if inventory_scale:
        scale,center=inventory_scale
        local=[(box['bounds'][i]-center[i%3])*scale+8 for i in range(6)]
    return {'from':local[:3], 'to':local[3:], 'shade':box['collision'], 'faces':{face:{'uv':[round(x,6) for x in face_uv(box['bounds'],clip,face)],'texture':'#'+box['faces'][face]} for face in FACES}}

def add_language(path, values):
    text=path.read_text(encoding='utf-8-sig')
    pattern=re.compile(r'"(?:\\.|[^"\\])*"|//[^\r\n]*|/\*[\s\S]*?\*/')
    clean=pattern.sub(lambda match:match.group() if match.group().startswith('"') else '',text)
    data=json.loads(clean)
    additions={k:v for k,v in values.items() if k not in data}
    for k,v in values.items():
        if k in data and data[k]!=v: raise ValueError('Language conflict: '+k)
    if not additions: return
    before=text.rstrip()
    assert before.endswith('}')
    body=before[:-1].rstrip()
    if data: body+=','
    lines=[json.dumps(k,ensure_ascii=False)+': '+json.dumps(v,ensure_ascii=False) for k,v in additions.items()]
    path.write_text(body+'\n  '+',\n  '.join(lines)+'\n}\n',encoding='utf-8')

def generate(source, resources, recipes):
    spec=read(recipes)
    namespace=resources/'assets/blacksouls'
    matnames=set()
    for m in spec['materials']:
        rect=m['rect'];im=Image.open(source/(m['sheet']+'.png')).convert('RGBA')
        assert len(rect)==4 and rect[2]>0 and rect[3]>0
        crop=im.crop((rect[0],rect[1],rect[0]+rect[2],rect[1]+rect[3]))
        if crop.getbbox() is None: raise ValueError('Empty source material: '+m['id'])
        if m.get('animation'):
            animation=m['animation']
            strip=Image.new('RGBA',(32,32*animation['count']))
            for frame in range(animation['count']):
                x=rect[0]+frame*animation['stride']
                image=im.crop((x,rect[1],x+rect[2],rect[1]+rect[3])).resize((32,32),Image.Resampling.NEAREST)
                strip.paste(image,(0,32*frame))
            output=namespace/'textures/block/original'/(m['id']+'.png')
            output.parent.mkdir(parents=True,exist_ok=True)
            strip.save(output)
            write(output.with_suffix('.png.mcmeta'),{'animation':{'frametime':animation['frametime'],'frames':[0,1,2,1]}})
            matnames.add(m['id'])
            continue
        pixels=[pixel for pixel in crop.getdata() if pixel[3]>200]
        if pixels:
            color=tuple(sorted(p[i] for p in pixels)[len(pixels)//2] for i in range(3))+(255,)
            background=Image.new('RGBA',crop.size,color)
            background.alpha_composite(crop)
            crop=background
        crop=crop.resize((32,32),Image.Resampling.NEAREST)
        output=namespace/'textures/block/original'/ (m['id']+'.png')
        output.parent.mkdir(parents=True,exist_ok=True)
        crop.save(output)
        matnames.add(m['id'])
    catalog=[];zh={};en={};tags={'axe':[],'pickaxe':[]};ids=set()
    for r in spec['blocks']:
        rid='original_'+r['id'];assert rid not in ids;ids.add(rid)
        assert all(v in matnames for v in r['textures'].values()),r
        boxes=mesh(r)
        assert all(all(math.isfinite(n) and 0<=n<=80 for n in b['bounds']) for b in boxes)
        assert all(all(b['bounds'][i]<b['bounds'][i+3] for i in range(3)) for b in boxes)
        maximum=[max(b['bounds'][i+3] for b in boxes) for i in range(3)]
        textures={name:'blacksouls:block/original/'+name for name in sorted(set(r['textures'].values()))}
        textures['particle']='blacksouls:block/original/'+r['textures']['body']
        parts=[]
        for y in range(math.ceil(maximum[1]/16)):
            for z in range(math.ceil(maximum[2]/16)):
                for x in range(math.ceil(maximum[0]/16)):
                    offset=[x,y,z];elements=[];collisions=[]
                    for box in boxes:
                        clip=clipped(box,offset)
                        if clip is None: continue
                        elements.append(model_element(box,clip,offset))
                        if box['collision']: collisions.append([round(clip[i]-offset[i%3]*16,5) for i in range(6)])
                    if not elements: continue
                    index=len(parts);key=rid+('_part_'+str(index) if index else '')
                    model='original/'+key
                    write(namespace/'models/block'/(model+'.json'),{'render_type':'minecraft:cutout','ambientocclusion':True,'textures':textures,'elements':elements})
                    variants={f'facing={f}':{'model':'blacksouls:block/'+model,**({'y':angle} if angle else {})} for f,angle in [('north',0),('east',90),('south',180),('west',270)]}
                    write(namespace/'blockstates'/(key+'.json'),{'variants':variants})
                    loot={'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'blacksouls:'+rid}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
                    write(resources/'data/blacksouls/loot_tables/blocks'/(key+'.json'),loot)
                    parts.append({'offset':offset,'collision':collisions})
                    tags['pickaxe' if r['material'] in ('stone','metal','gold') else 'axe'].append('blacksouls:'+key)
                    zh['block.blacksouls.'+key]=r['zh']
                    en['block.blacksouls.'+key]=r['en']
        assert parts[0]['offset']==[0,0,0],rid
        center=[v/2 for v in maximum];scale=15/max(maximum)
        inventory=[model_element(b,b['bounds'],[0,0,0],(scale,center)) for b in boxes]
        display={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[0.85,0.85,0.85]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[0.5,0.5,0.5]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[0.75,0.75,0.75]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[0.5,0.5,0.5]},'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[0.6,0.6,0.6]}}
        write(namespace/'models/item'/(rid+'.json'),{'render_type':'minecraft:cutout','textures':textures,'elements':inventory,'display':display})
        catalog.append({'id':rid,'material':r['material'],'strength':r.get('strength',1.2),'light':r.get('light',0),'solid':r['template']=='cube','parts':parts,'source':r['source'],'category':r['category']})
    write(resources/'data/blacksouls/original_blocks.json',{'version':1,'region':'rabbit_hole_library','blocks':catalog})
    for tool,values in tags.items():
        path=resources/'data/minecraft/tags/blocks/mineable'/(tool+'.json')
        existing=read(path) if path.exists() else {'replace':False,'values':[]}
        existing['values']=list(dict.fromkeys(existing['values']+values))
        write(path,existing)
    zh['itemGroup.blacksouls.original_building_blocks']='BLACKSOULS · 原作布景'
    en['itemGroup.blacksouls.original_building_blocks']='BLACKSOULS · Original Scenery'
    add_language(namespace/'lang/zh_cn.json',zh)
    add_language(namespace/'lang/en_us.json',en)
    print(json.dumps({'objects':len(catalog),'occupied_parts':sum(len(x['parts']) for x in catalog),'source_materials':len(matnames),'geometry_cuboids':sum(len(mesh(r)) for r in spec['blocks'])},ensure_ascii=False))

if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--source',type=Path,default=Path('D:/BLACK SOULS II/Graphics/Tilesets'))
    parser.add_argument('--resources',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/resources')
    parser.add_argument('--recipes',type=Path,default=Path(__file__).with_name('original_blocks.json'))
    args=parser.parse_args()
    generate(args.source,args.resources,args.recipes)
