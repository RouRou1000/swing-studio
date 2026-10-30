import math, sys
from PIL import Image
N = 32
C = {
 'bg': (38, 33, 56), 'bg2': (46, 40, 68), 'rim': (22, 19, 33), 'hi': (70, 62, 98),
 'o': (18, 14, 24),
 'b1': (236, 244, 248), 'b2': (164, 196, 214), 'b3': (98, 128, 152),
 'g1': (247, 205, 92), 'g2': (196, 140, 46),
 'h1': (128, 84, 52), 'h2': (86, 54, 34),
 's1': (255, 255, 255), 's2': (196, 230, 246), 's3': (120, 178, 214),
}
px = {}
def put(x, y, c): px[(x, y)] = c

for y in range(N):
    for x in range(N):
        cut = min(x, N-1-x) + min(y, N-1-y)
        if cut < 2 and min(x, N-1-x) < 2 and min(y, N-1-y) < 2 and (min(x,N-1-x)+min(y,N-1-y)) < 2:
            continue
        edge = x in (0, N-1) or y in (0, N-1) or (min(x,N-1-x)==1 and min(y,N-1-y)==1)
        if edge:
            put(x, y, 'rim'); continue
        c = 'bg'
        put(x, y, c)
for x in range(2, N-2):
    put(x, 1, 'hi')
for y in range(2, N-2):
    put(1, y, 'hi')

cx, cy = 11.5, 22.5
a0, a1 = math.radians(168), math.radians(58)
span = (a0 - a1)
for y in range(2, N-2):
    for x in range(2, N-2):
        dx, dy = x + .5 - cx, cy - (y + .5)
        r = math.hypot(dx, dy)
        a = math.atan2(dy, dx) % (2*math.pi)
        t = (a0 - a) / span
        if not 0 <= t <= 1:
            continue
        outer = 18.6
        thick = 0.8 + 5.2 * t ** 1.3
        if outer - thick <= r <= outer:
            depth = (outer - r) / thick
            if t < 0.35 and (x + y) % 2:
                continue
            if t < 0.15 and (x % 2 or y % 2):
                continue
            c = 's1' if depth < 0.34 else 's2' if depth < 0.7 else 's3'
            if t < 0.5 and c == 's1':
                c = 's2'
            put(x, y, c)

spr = {}
gx, gy = 11, 22
L = 12
for k in range(1, L):
    x, y = gx + k, gy - k
    spr[(x, y)] = 'b1'
    spr[(x + 1, y)] = 'b2'
    spr[(x + 1, y + 1)] = 'b3'
    spr[(x, y + 1)] = 'b2'
spr[(gx + L, gy - L)] = 'b1'
spr[(gx + L, gy - L + 1)] = 'b2'
for k in range(-3, 4):
    x, y = gx + k, gy + k
    spr[(x, y)] = 'g1'
    spr[(x + 1, y)] = 'g2'
    spr[(x, y + 1)] = 'g2'
for k in range(1, 5):
    spr[(gx - k, gy + k + 1)] = 'h1' if k % 2 else 'h2'
    spr[(gx - k + 1, gy + k + 1)] = 'h2'
spr[(gx - 5, gy + 6)] = 'g1'
spr[(gx - 4, gy + 6)] = 'g2'
spr[(gx - 5, gy + 7)] = 'g2'
spr[(gx - 4, gy + 7)] = 'g2'
outline = set()
for (x, y) in spr:
    for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
        q = (x+dx, y+dy)
        if q not in spr:
            outline.add(q)
for q in outline:
    put(*q, 'o')
for q, c in spr.items():
    put(*q, c)

img = Image.new('RGBA', (N, N), (0, 0, 0, 0))
for (x, y), c in px.items():
    if 0 <= x < N and 0 <= y < N:
        img.putpixel((x, y), C[c] + (255,))
out = sys.argv[1]
img.resize((512, 512), Image.NEAREST).save(out + '/icon_512.png')
img.resize((256, 256), Image.NEAREST).save(out + '/icon_256.png')
img.resize((128, 128), Image.NEAREST).save(out + '/icon_128.png')
