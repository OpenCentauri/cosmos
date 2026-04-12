#!/usr/bin/env python3
import os, configparser, sys

VALIDATORS = {
    'ui': {
        'screen_ui': ['grumpyscreen', 'guppyscreen', 'atomscreen'],
        'web_ui': ['mainsail', 'fluidd'],
    },
    'update': {
        'release': ['stable', 'nightly'],
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

def merge_configs(default_config : dict, user_config : dict) -> dict:
    for section, options in user_config.items():
        if section not in default_config:
            default_config[section] = {}
        default_config[section].update(options)
    return default_config

def main(section : str, option : str):
    default_config = load_config(DEFAULT_CONFIG_PATH)
    user_config = load_config(VARIABLE_CONFIG_PATH)
    validate_config(user_config)
    merged_config = merge_configs(default_config, user_config)

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
    