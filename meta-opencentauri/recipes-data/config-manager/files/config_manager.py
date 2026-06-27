#!/usr/bin/env python3
import os, configparser, sys

VALIDATORS = {
    'ui': {
        'screen_ui': ['grumpyscreen', 'guppyscreen', 'atomscreen', 'none'],
        'web_ui': ['mainsail', 'fluidd'],
        'screen_brightness': [str(i) for i in range(101)],
    },
    'update': {
        'release': ['stable', 'nightly'],
        'check_for_updates': ['True', 'False'],
    },
    'flash_mcu': {
        'elegoo_canvas': ['True', 'False'],
    },
    'klipper': {
        'sync_camera_led_to_chamber_led': ['True', 'False'],
        'camera_led_default_on': ['True', 'False'],
        'bypass_calibration': ['True', 'False'],
        'heatsoak': [str(x / 10) for x in range(100)] + [str(x) for x in range(31)],
        'adaptive_mesh': ['True', 'False'],
        'adaptive_purge': ['True', 'False'],
        'unload_after_print': ['True', 'False'],
        'nozzle_z_homing': ['True', 'False'],
        'z_ideal_lifting_distance': [str(i) for i in range(257)],
        'full_calibrate_hotend_temperature': [str(i) for i in range(200, 301)],
        'full_calibrate_bed_temperature': [str(i) for i in range(30, 111)],
        'toolhead_led': ['True', 'False'],
    },
}

VARIABLE_CONFIG_PATH = '/etc/klipper/config/cosmos.conf'
DEFAULT_CONFIG_PATH = '/usr/share/config-manager/default.conf'

def validate_config(config : dict):
    for section, options in VALIDATORS.items():
        if section not in config:
            continue
        for option, valid_values in list(options.items()):
            if option not in config[section]:
                continue
            value = config[section][option]
            if value not in valid_values:
                print(f"Warning: Invalid value '{value}' for '{option}' in section '{section}'. Valid options are: {valid_values}", file=sys.stderr)
                del config[section][option]

def load_config(path : str) -> dict:
    parser = configparser.ConfigParser()
    if os.path.exists(path):
        parser.read(path)
    else:
        print(f"Warning: Config file '{path}' not found. Using empty config.", file=sys.stderr)

    return {str(section): dict(parser.items(section)) for section in parser.sections()}

def load_config_comments(path : str) -> dict:
    comments = {}
    current_section = None
    previous_line = ''

    if not os.path.exists(path):
        return comments

    with open(path, 'r', encoding='utf-8') as config_file:
        for raw_line in config_file:
            line = raw_line.strip()

            if line.startswith('[') and line.endswith(']'):
                current_section = line[1:-1].strip()
                comments.setdefault(current_section, {})
            elif current_section and '=' in line and not line.startswith(('#', ';')):
                option = line.split('=', 1)[0].strip()
                if previous_line.lstrip().startswith('#'):
                    comments.setdefault(current_section, {})[option] = previous_line.rstrip('\r\n')

            previous_line = raw_line

    return comments

def load_config_header(path : str) -> list:
    header = []

    if not os.path.exists(path):
        return header

    with open(path, 'r', encoding='utf-8') as config_file:
        for raw_line in config_file:
            line = raw_line.strip()

            if raw_line.lstrip().startswith('#') or (header and line == ''):
                header.append(raw_line.rstrip('\r\n'))
                continue

            break

    return header

def render_config(config : dict, comments : dict, header : list = None) -> str:
    lines = []

    if header:
        lines.extend(header)

    for section, options in config.items():
        if lines and lines[-1] != '':
            lines.append('')
        lines.append(f'[{section}]')

        section_comments = comments.get(section, {})
        for option, value in options.items():
            comment = section_comments.get(option)
            if comment:
                lines.append(comment)
            lines.append(f'{option} = {value}')

    return '\n'.join(lines) + '\n'

def save_config(path : str, config : dict, comments : dict, header : list = None):
    content = render_config(config, comments, header)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as config_file:
        config_file.write(content)

def config_needs_update(path : str, config : dict, comments : dict, header : list = None) -> bool:
    if not os.path.exists(path):
        return True

    with open(path, 'r', encoding='utf-8') as config_file:
        return config_file.read() != render_config(config, comments, header)

def merge_configs(default_config : dict, user_config : dict) -> dict:
    for section, options in user_config.items():
        if section not in default_config:
            default_config[section] = {}
        default_config[section].update(options)
    return default_config

def main(section : str, option : str):
    default_config = load_config(DEFAULT_CONFIG_PATH)
    default_comments = load_config_comments(DEFAULT_CONFIG_PATH)
    default_header = load_config_header(DEFAULT_CONFIG_PATH)
    user_config = load_config(VARIABLE_CONFIG_PATH)
    validate_config(user_config)
    merged_config = merge_configs(default_config, user_config)

    if user_config != merged_config or config_needs_update(VARIABLE_CONFIG_PATH, merged_config, default_comments, default_header):
        save_config(VARIABLE_CONFIG_PATH, merged_config, default_comments, default_header)

    if section not in merged_config or option not in merged_config[section]:
        raise ValueError(f"Option '{option}' not found in section '{section}'")

    print(merged_config[section][option], end='')

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: config_manager <section> <option>", file=sys.stderr)
        sys.exit(1)

    section = sys.argv[1]
    option = sys.argv[2]

    try:
        main(section, option)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
