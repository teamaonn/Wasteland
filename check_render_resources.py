"""Check runtime resource bindings using the bundled GeckoLib loader's regex constants.
Usage: python check_render_resources.py MOD.jar GECKOLIB.jar
This checks lookup IDs, required clips and textures; it does not render the game.
"""
import json,re,struct,sys,zipfile

def constants(data):
    count=struct.unpack_from('>H',data,8)[0];p=10;i=1;strings=[]
    while i<count:
        tag=data[p];p+=1
        if tag==1:
            size=struct.unpack_from('>H',data,p)[0];p+=2;strings.append(data[p:p+size].decode('utf-8'));p+=size
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):p+=2
        elif tag in (9,10,11,12,17,18):p+=4
        elif tag==15:p+=3
        else:raise AssertionError(('bad constant pool tag',tag))
        i+=1
    return strings

with zipfile.ZipFile(sys.argv[1]) as mod,zipfile.ZipFile(sys.argv[2]) as gecko:
    strings=constants(gecko.read('com/geckolib/cache/GeckoLibResources.class'))
    prefix=next(s for s in strings if s.startswith('^(geckolib/)'))
    suffix=next(s for s in strings if s.startswith('((\\.geo)'))
    def key(path):return re.sub(suffix,'',re.sub(prefix,'',path))
    models={key(n.removeprefix('assets/wasteland/')):n for n in mod.namelist() if n.startswith('assets/wasteland/geckolib/models/') and n.endswith('.json')}
    animations={key(n.removeprefix('assets/wasteland/')):json.loads(mod.read(n))['animations'] for n in mod.namelist() if n.startswith('assets/wasteland/geckolib/animations/') and n.endswith('.json')}
    renderer=constants(mod.read('com/joshua/wasteland/client/CreatureModel.class'))
    assert 'entity/\x01' in renderer
    assert not any(s.startswith(('geckolib/models/entity/','geckolib/animations/entity/')) for s in renderer)
    required={'deathclaw':{'idle','walk','run','swipe','slam','roar','leap','climb'},'deathclaw_nuka':{'idle','walk','run','swipe','slam','roar','leap','climb'},'ghoul':{'idle','walk','run','attack'},'protectron':{'idle','walk','melee'},'mothman':{'idle','walk','attack'}}
    for name,clips in required.items():
        identifier='entity/'+name
        assert identifier in models,('missing model',identifier)
        assert identifier in animations,('missing animation file',identifier)
        assert clips<=animations[identifier].keys(),('missing clips',name)
        print('PASS: model and all referenced clips resolve for wasteland:'+identifier)
    textures=['deathclaw']+['deathclaw_nuka_'+str(i) for i in range(1,4)]+['ghoul_'+str(i) for i in range(12)]+['protectron','mothman']
    for name in textures:assert 'assets/wasteland/textures/entity/'+name+'.png' in mod.namelist(),name
    print('PASS: all 18 Gecko creature appearance textures exist')
    for i in range(6):assert f'assets/wasteland/textures/entity/raider_{i}.png' in mod.namelist()
    print('PASS: all six humanoid raider textures exist')
    print('PASS: custom model and animation IDs have no legacy path prefixes or extensions')
    print('8 resource-binding checks passed using GeckoLib '+json.loads(gecko.read('fabric.mod.json'))['version']+' loader normalization.')
