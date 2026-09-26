# Motion runout from an Elegoo Centauri 2 filament sensor
#
# Copyright (C) 2026  James Turton <james.turton@gmx.com>
#
# This file may be distributed under the terms of the GNU GPLv3 license.

from .canvas_filament_sensor import CanvasMotionSensor


def load_config_prefix(config):
    return CanvasMotionSensor(config)
