#!/usr/bin/env python3
"""Generate soldier.png (64x64 skin) and logo.png for the TACZ Soldiers mod. Pure stdlib."""
import struct, zlib, random, math, os

def write_png(path, w, h, rgba_rows):
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(v for px in row for v in px) for row in rgba_rows)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(raw, 9))
    png += chunk(b'IEND', b'')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(png)
    print('wrote', path, len(png), 'bytes')

def blank(w, h):
    return [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

def fill(img, x, y, w, h, color):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if 0 <= xx < len(img[0]) and 0 <= yy < len(img):
                img[yy][xx] = color

def noise(img, x, y, w, h, palette, rnd, chance=0.55):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if rnd.random() < chance and 0 <= xx < len(img[0]) and 0 <= yy < len(img):
                img[yy][xx] = palette[rnd.randrange(len(palette))]

rnd = random.Random(42)

# ---- Palette ----
HELMET = [(62, 70, 44), (55, 63, 40), (68, 76, 48)]        # olive helmet
FACE   = [(216, 176, 140), (205, 166, 132), (222, 184, 148)]  # skin
CAMO   = [(74, 83, 52), (63, 72, 45), (89, 97, 63), (54, 61, 40)]  # camo green
PANTS  = [(52, 56, 44), (46, 50, 40), (60, 63, 50)]        # dark pants
BOOTS  = [(30, 28, 24), (38, 34, 28)]
STRAP  = (35, 32, 26, 255)
EYE_W  = (240, 240, 240, 255)
EYE_P  = (30, 40, 60, 255)

img = blank(64, 64)

def paint_box(img, x, y, w, h, base, palette, rnd):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            img[yy][xx] = base + (0 if isinstance(base, tuple) and len(base) == 4 else 255,) if False else base if len(base) == 4 else (*base, 255)
    noise(img, x, y, w, h, [(*c, 255) for c in palette], rnd, 0.35)

# Head: top/bottom/sides
fill(img, 8, 0, 8, 8, (*HELMET[0], 255))   # top
fill(img, 16, 0, 8, 8, (*HELMET[1], 255))  # bottom
fill(img, 0, 8, 8, 8, (*HELMET[1], 255))   # right side
fill(img, 8, 8, 8, 8, (*FACE[0], 255))     # front face
fill(img, 16, 8, 8, 8, (*HELMET[1], 255))  # left side
fill(img, 24, 8, 8, 8, (*HELMET[2], 255))  # back
noise(img, 8, 0, 8, 8, [(*c, 255) for c in HELMET], rnd, 0.4)
noise(img, 24, 8, 8, 8, [(*c, 255) for c in HELMET], rnd, 0.4)
noise(img, 0, 8, 8, 8, [(*c, 255) for c in HELMET], rnd, 0.4)
noise(img, 16, 8, 8, 8, [(*c, 255) for c in HELMET], rnd, 0.4)

# Face details (front: x 8..16, y 8..16)
for xx in range(8, 16):  # helmet rim
    img[8][xx] = (*HELMET[1], 255)
for xx in range(8, 16):  # strap
    img[13][xx] = STRAP
img[11][10] = EYE_W; img[11][11] = EYE_P
img[11][13] = EYE_W; img[11][14] = EYE_P
img[12][12] = (190, 150, 118, 255); img[13][12] = (190, 150, 118, 255)  # nose-ish

# Body: top(20,16,8,4) bottom(28,16,8,4) right(16,20,4,12) front(20,20,8,12) left(28,20,4,12) back(32,20,8,12)
fill(img, 20, 16, 8, 4, (*CAMO[2], 255))
fill(img, 28, 16, 8, 4, (*CAMO[3], 255))
fill(img, 16, 20, 4, 12, (*CAMO[1], 255))
fill(img, 20, 20, 8, 12, (*CAMO[0], 255))
fill(img, 28, 20, 4, 12, (*CAMO[1], 255))
fill(img, 32, 20, 8, 12, (*CAMO[2], 255))
noise(img, 16, 20, 24, 12, [(*c, 255) for c in CAMO], rnd, 0.45)
# chest rig / straps
for yy in range(21, 31):
    img[yy][22] = STRAP
    img[yy][25] = STRAP
for xx in range(20, 28):
    img[24][xx] = STRAP

# Right arm: top(44,16,4,4) bottom(48,16,4,4) right(40,20,4,12) front(44,20,4,12) left(48,20,4,12) back(52,20,4,12)
fill(img, 44, 16, 4, 4, (*CAMO[1], 255))
fill(img, 48, 16, 4, 4, (*CAMO[3], 255))
fill(img, 40, 20, 4, 12, (*CAMO[1], 255))
fill(img, 44, 20, 4, 12, (*CAMO[0], 255))
fill(img, 48, 20, 4, 12, (*CAMO[1], 255))
fill(img, 52, 20, 4, 12, (*CAMO[2], 255))
noise(img, 40, 20, 16, 12, [(*c, 255) for c in CAMO], rnd, 0.4)
# gloves
for xx in range(40, 56):
    for yy in range(30, 32):
        img[yy][xx] = (28, 26, 22, 255)

# Right leg: top(4,16,4,4) bottom(8,16,4,4) right(0,20,4,12) front(4,20,4,12) left(8,20,4,12) back(12,20,4,12)
fill(img, 4, 16, 4, 4, (*PANTS[1], 255))
fill(img, 8, 16, 4, 4, (*PANTS[2], 255))
fill(img, 0, 20, 4, 12, (*PANTS[1], 255))
fill(img, 4, 20, 4, 12, (*PANTS[0], 255))
fill(img, 8, 20, 4, 12, (*PANTS[1], 255))
fill(img, 12, 20, 4, 12, (*PANTS[2], 255))
noise(img, 0, 20, 16, 9, [(*c, 255) for c in PANTS], rnd, 0.35)
for xx in range(0, 16):
    for yy in range(29, 32):
        img[yy][xx] = (*BOOTS[rnd.randrange(2)], 255)

# Left arm (32,48..): top(36,48,4,4) bottom(40,48,4,4) right(32,52,4,12) front(36,52,4,12) left(40,52,4,12) back(44,52,4,12)
fill(img, 36, 48, 4, 4, (*CAMO[1], 255))
fill(img, 40, 48, 4, 4, (*CAMO[3], 255))
fill(img, 32, 52, 4, 12, (*CAMO[1], 255))
fill(img, 36, 52, 4, 12, (*CAMO[0], 255))
fill(img, 40, 52, 4, 12, (*CAMO[1], 255))
fill(img, 44, 52, 4, 12, (*CAMO[2], 255))
noise(img, 32, 52, 16, 10, [(*c, 255) for c in CAMO], rnd, 0.4)
for xx in range(32, 48):
    for yy in range(62, 64):
        img[yy][xx] = (28, 26, 22, 255)

# Left leg (16,48..): top(20,48,4,4) bottom(24,48,4,4) right(16,52,4,12) front(20,52,4,12) left(24,52,4,12) back(28,52,4,12)
fill(img, 20, 48, 4, 4, (*PANTS[1], 255))
fill(img, 24, 48, 4, 4, (*PANTS[2], 255))
fill(img, 16, 52, 4, 12, (*PANTS[1], 255))
fill(img, 20, 52, 4, 12, (*PANTS[0], 255))
fill(img, 24, 52, 4, 12, (*PANTS[1], 255))
fill(img, 28, 52, 4, 12, (*PANTS[2], 255))
noise(img, 16, 52, 16, 9, [(*c, 255) for c in PANTS], rnd, 0.35)
for xx in range(16, 32):
    for yy in range(61, 64):
        img[yy][xx] = (*BOOTS[rnd.randrange(2)], 255)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out_dir = os.path.join(ROOT, 'src/main/resources/assets/taczsoldiers/textures/entity')
write_png(os.path.join(out_dir, 'soldier.png'), 64, 64, img)

# ---- Logo 128x128 ----
logo = blank(128, 128)
for y in range(128):
    for x in range(128):
        g = 40 + int(30 * (y / 128))
        logo[y][x] = (g // 2, g, g // 3, 255)
# border
for i in range(128):
    for t in range(4):
        logo[t][i] = (200, 210, 180, 255); logo[127 - t][i] = (200, 210, 180, 255)
        logo[i][t] = (200, 210, 180, 255); logo[i][127 - t] = (200, 210, 180, 255)
# five-pointed star
cx, cy, R = 64, 66, 44
pts = []
for k in range(5):
    ang = -math.pi / 2 + k * 2 * math.pi / 5
    pts.append((cx + R * math.cos(ang), cy + R * math.sin(ang)))
    ang2 = ang + math.pi / 5
    pts.append((cx + R * 0.42 * math.cos(ang2), cy + R * 0.42 * math.sin(ang2)))

def inside(px, py):
    cnt = 0
    n = len(pts)
    for i in range(n):
        x1, y1 = pts[i]; x2, y2 = pts[(i + 1) % n]
        if (y1 > py) != (y2 > py):
            xt = x1 + (py - y1) * (x2 - x1) / (y2 - y1)
            if xt > px:
                cnt += 1
    return cnt % 2 == 1

for y in range(128):
    for x in range(128):
        if inside(x, y):
            logo[y][x] = (228, 222, 200, 255)
logo_dir = os.path.join(ROOT, 'src/main/resources')
write_png(os.path.join(logo_dir, 'logo.png'), 128, 128, logo)
print('done')
