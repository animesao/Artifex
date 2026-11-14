import os
import json
import shutil
import yaml

SERVER_PACK = r'D:\server\Artifex-ResourcePack'
CLIENT_PACK = r'C:\Users\Anime\AppData\Roaming\ElyPrismLauncher\instances\26.3\minecraft\resourcepacks\Artifex-ResourcePack'
PROTOTYPE_YML = r'D:\server\plugins\Artifex\items\resource_forge_prototype.yml'
EXPANSION_YML = r'D:\server\plugins\Artifex\items\resource_forge_expansion.yml'

ITEMS_MAP = {
    # Prototype
    'ember_ore_raw': {'base': 'raw_copper', 'cmd': 2001, 'model': 'artifex:item/ember_ore_raw'},
    'ember_shard': {'base': 'fire_charge', 'cmd': 2002, 'model': 'artifex:item/ember_shard'},
    'ember_ingot': {'base': 'copper_ingot', 'cmd': 2003, 'model': 'artifex:item/ember_ingot'},
    'ember_ore': {'base': 'note_block', 'cmd': 2201, 'model': 'artifex:item/ore_ember'},
    'ember_block': {'base': 'note_block', 'cmd': 2205, 'model': 'artifex:item/block_ember'},
    
    'frost_ore_raw': {'base': 'raw_iron', 'cmd': 2019, 'model': 'artifex:item/frost_ore_raw'},
    'frost_crystal_shard': {'base': 'amethyst_shard', 'cmd': 2020, 'model': 'artifex:item/frost_crystal_shard'},
    'frost_ingot': {'base': 'iron_ingot', 'cmd': 2021, 'model': 'artifex:item/frost_ingot'},
    'frost_ore': {'base': 'note_block', 'cmd': 2202, 'model': 'artifex:item/ore_frost'},
    'frost_block': {'base': 'note_block', 'cmd': 2214, 'model': 'artifex:item/block_frost'},
    
    'storm_shard': {'base': 'lapis_lazuli', 'cmd': 2037, 'model': 'artifex:item/storm_shard'},
    'storm_ingot': {'base': 'gold_ingot', 'cmd': 2038, 'model': 'artifex:item/storm_ingot'},
    'storm_ore': {'base': 'note_block', 'cmd': 2203, 'model': 'artifex:item/ore_storm'},
    'storm_block': {'base': 'note_block', 'cmd': 2215, 'model': 'artifex:item/block_storm'},
    
    'void_shard': {'base': 'echo_shard', 'cmd': 2053, 'model': 'artifex:item/void_shard'},
    'void_ingot': {'base': 'netherite_ingot', 'cmd': 2054, 'model': 'artifex:item/void_ingot'},
    'void_ore': {'base': 'note_block', 'cmd': 2204, 'model': 'artifex:item/ore_void'},
    'void_block': {'base': 'note_block', 'cmd': 2216, 'model': 'artifex:item/block_void'},
    
    # Expansion
    'ruby_ore': {'base': 'note_block', 'cmd': 2206, 'model': 'artifex:item/ore_ruby'},
    'ruby_rough': {'base': 'redstone', 'cmd': 2086, 'model': 'artifex:item/ruby_rough'},
    'ruby_flawless': {'base': 'diamond', 'cmd': 2087, 'model': 'artifex:item/ruby_flawless'},
    'ruby_block': {'base': 'note_block', 'cmd': 2207, 'model': 'artifex:item/block_ruby'},
    
    'sapphire_ore': {'base': 'note_block', 'cmd': 2208, 'model': 'artifex:item/ore_sapphire'},
    'sapphire_rough': {'base': 'lapis_lazuli', 'cmd': 2088, 'model': 'artifex:item/sapphire_rough'},
    'sapphire_flawless': {'base': 'diamond', 'cmd': 2089, 'model': 'artifex:item/sapphire_flawless'},
    'sapphire_block': {'base': 'note_block', 'cmd': 2209, 'model': 'artifex:item/block_sapphire'},
    
    'topaz_ore': {'base': 'note_block', 'cmd': 2210, 'model': 'artifex:item/ore_topaz'},
    'topaz_rough': {'base': 'glowstone_dust', 'cmd': 2090, 'model': 'artifex:item/topaz_rough'},
    'topaz_flawless': {'base': 'diamond', 'cmd': 2091, 'model': 'artifex:item/topaz_flawless'},
    
    'adamantite_ore': {'base': 'note_block', 'cmd': 2211, 'model': 'artifex:item/ore_adamantite'},
    'adamantite_ore_raw': {'base': 'raw_copper', 'cmd': 2112, 'model': 'artifex:item/adamantite_ore_raw'},
    'adamantite_ingot': {'base': 'netherite_ingot', 'cmd': 2113, 'model': 'artifex:item/adamantite_ingot'},
    
    'mithril_ore': {'base': 'note_block', 'cmd': 2212, 'model': 'artifex:item/ore_mithril'},
    'mithril_ore_raw': {'base': 'raw_iron', 'cmd': 2114, 'model': 'artifex:item/mithril_ore_raw'},
    'mithril_ingot': {'base': 'iron_ingot', 'cmd': 2115, 'model': 'artifex:item/mithril_ingot'},
    
    'ancient_runestone': {'base': 'note_block', 'cmd': 2213, 'model': 'artifex:item/ancient_runestone'},
    'runestone_slate': {'base': 'paper', 'cmd': 2071, 'model': 'artifex:item/runestone_slate'},
    'rune_of_power': {'base': 'redstone', 'cmd': 2072, 'model': 'artifex:item/rune_of_power'},
    'rune_of_protection': {'base': 'lapis_lazuli', 'cmd': 2073, 'model': 'artifex:item/rune_of_protection'},
    'rune_of_haste': {'base': 'glowstone_dust', 'cmd': 2074, 'model': 'artifex:item/rune_of_haste'},
    'rune_of_vitality': {'base': 'emerald', 'cmd': 2075, 'model': 'artifex:item/rune_of_vitality'},
    
    'ember_blade': {'base': 'netherite_sword', 'cmd': 1025, 'model': 'artifex:item/ember_blade'},
    'ember_pickaxe': {'base': 'netherite_pickaxe', 'cmd': 1026, 'model': 'artifex:item/ember_pickaxe'},
    'frost_battleaxe': {'base': 'netherite_axe', 'cmd': 1027, 'model': 'artifex:item/frost_battleaxe'},
    'storm_warhammer': {'base': 'netherite_axe', 'cmd': 1028, 'model': 'artifex:item/storm_warhammer'},
    'ruby_amulet': {'base': 'chainmail_chestplate', 'cmd': 1029, 'model': 'artifex:item/ruby_amulet'},
    'sapphire_ring': {'base': 'diamond', 'cmd': 1030, 'model': 'artifex:item/sapphire_ring'}
}

JAVA_ITEMS = [
    (1001, 'katana_drawn'),
    (1002, 'katana_sheathed'),
    (1003, 'storm_staff'),
    (1004, 'shadow_dagger'),
    (1005, 'abyssal_amulet'),
    (1006, 'soul_reaper'),
    (1007, 'storm_spear'),
    (1008, 'frost_dagger'),
    (1009, 'infernal_staff'),
    (1010, 'nature_ring'),
    (1011, 'wind_pendant'),
    (1012, 'eye_of_the_abyss'),
    (1013, 'phoenix_feather'),
    (1014, 'crystal_drill'),
    (1015, 'builders_wand'),
    (1016, 'treasure_seeker'),
    (1017, 'artisan_hammer'),
    (1018, 'recall_scroll'),
    (1019, 'frost_bomb'),
    (1020, 'berserker_elixir'),
    (1021, 'ancient_seed'),
    (1022, 'dragon_scale'),
    (1023, 'void_essence'),
    (1024, 'ancient_ingot')
]

for cmd, name in JAVA_ITEMS:
    ITEMS_MAP[name] = {'base': 'netherite_sword', 'cmd': cmd, 'model': f'artifex:item/{name}'}

def main():
    for pack_root in [SERVER_PACK, CLIENT_PACK]:
        if not os.path.exists(pack_root):
            continue
        # 1. Update pack.mcmeta
        mcmeta_path = os.path.join(pack_root, 'pack.mcmeta')
        mcmeta_data = {
            'pack': {
                'pack_format': 65,
                'min_format': 34,
                'max_format': 120,
                'description': '\u00a7b\u00a7lARTIFEX \u00a77\u2022 \u00a7fCustom 3D Weapons, Ores & RPG Ecosystem'
            }
        }
        with open(mcmeta_path, 'w', encoding='utf-8') as f:
            json.dump(mcmeta_data, f, indent=2)
        
        # 2. Generate assets/artifex/items/<item_id>.json
        artifex_items_dir = os.path.join(pack_root, 'assets', 'artifex', 'items')
        os.makedirs(artifex_items_dir, exist_ok=True)
        
        for item_id, info in ITEMS_MAP.items():
            item_def = {
                'model': {
                    'type': 'minecraft:model',
                    'model': info['model']
                }
            }
            with open(os.path.join(artifex_items_dir, f'{item_id}.json'), 'w', encoding='utf-8') as f:
                json.dump(item_def, f, indent=2)
                
        # 3. Generate assets/minecraft/items/<vanilla_base>.json
        minecraft_items_dir = os.path.join(pack_root, 'assets', 'minecraft', 'items')
        os.makedirs(minecraft_items_dir, exist_ok=True)
        
        by_base = {}
        for item_id, info in ITEMS_MAP.items():
            by_base.setdefault(info['base'], []).append((info['cmd'], info['model']))
            
        for base, entries in by_base.items():
            entries.sort(key=lambda x: x[0])
            fallback_model = f'minecraft:block/{base}' if base == 'note_block' else f'minecraft:item/{base}'
            
            dispatch_entries = []
            for cmd, model in entries:
                dispatch_entries.append({
                    'threshold': cmd,
                    'model': {
                        'type': 'minecraft:model',
                        'model': model
                    }
                })
                
            vanilla_def = {
                'model': {
                    'type': 'minecraft:range_dispatch',
                    'property': 'minecraft:custom_model_data',
                    'index': 0,
                    'fallback': {
                        'type': 'minecraft:model',
                        'model': fallback_model
                    },
                    'entries': dispatch_entries
                }
            }
            with open(os.path.join(minecraft_items_dir, f'{base}.json'), 'w', encoding='utf-8') as f:
                json.dump(vanilla_def, f, indent=2)

    print('Modern 1.21.4+ / 26.3 item definitions successfully generated!')

if __name__ == '__main__':
    main()
