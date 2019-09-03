import sys
import datetime
import imageio
import glob
import cv2

import os
import numpy as np
from PIL import Image, ImageFont, ImageDraw
import moviepy.editor as mpy
VALID_EXTENSIONS = ('png', 'jpg')


def create_gif(filenames, duration):
    images = []
    counter =0
    maxLength = len(filenames)
    #font = ImageFont.truetype("C:\\Windows\\WinSxS\\amd64_microsoft-windows-font-truetype-yugothic_31bf3856ad364e35_10.0.17134.1_none_be6df5bb828d2629\\YuGothR.ttc", 25)
   
    font = ImageFont.truetype("/Library/Fonts/Andale Mono.ttf", 15)
    for filename in filenames:
        counter = counter +1
        print (str(counter) + '/' +str(maxLength))

        #img = imageio.imread(filename)
        img = Image.open(filename)
        #img = img.resize((640,480), Image.ANTIALIAS)
        #img =  cv2.cvtColor(img,cv2.COLOR_GRAY2RGB)
        
        #img = cv2.blur(img,(3,3))



        #img = Image.fromarray(img)
        draw = ImageDraw.Draw(img)
        #draw.text((0,0), str(counter) + '/' +str(maxLength), (255,0,0), font=font)

        img = np.array(img)



        #original = imageio.imread('/Users/ryanspick/Dropbox/York/GAN/spatial_gan-master/data/1.png')
        #original = cv2.cvtColor(original,cv2.COLOR_GRAY2RGB)
        #original = cv2.resize(original, dsize=(2434,2434), interpolation=cv2.INTER_CUBIC)
        #original = np.resize(original,img.shape )

        #img = np.hstack((img,original))

        images.append(img)

    fps = duration
    clip = mpy.ImageSequenceClip(images, fps=fps)
    clip.write_gif('{}.gif'.format('blured'), fps=fps, loop = False)


if __name__ == "__main__":
    script = sys.argv.pop(0)
    #first arg is time between transition in miliseconds/ second arg is path to image location (glob will get all)
    if len(sys.argv) < 2:
        print('Usage: python {} <fps> <path to images separated by space>'.format(script))
        sys.exit(1)
    
    duration = float(sys.argv.pop(0))

    path = sys.argv.pop(0) 
    print(path)
    types = ('*.jpg', '*.png','*eps')

    files_grabbed = []
    for files in types:
        files_grabbed.extend(glob.glob(path + files))

    def sortKey(s):
        print(s)
        return int(s.split('/')[1].split('.')[0].replace('foo',''))

    print(files_grabbed)
    #files_grabbed.sort(key=os.path.getmtime)
    files_grabbed.sort(key=sortKey)

    #files_grabbed = files_grabbed[::2]
    print(files_grabbed)

    #files_grabbed.extend(files_grabbed[::-1])
    create_gif(files_grabbed, duration)