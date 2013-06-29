
# Comment Search Plug-In for OmegaT

This plug-in is intended to use during translating PO files for 
resource strings of Apache OpenOffice.

If you translate PO files on OmegaT[1], try this plug-in to find 
the correct location of the specific string.
This plug-in works with "hhacker"[2] tool.

The plug-in starts HTTP sever to listen for search requests. 
Therefore you can trigger search function from out of the 
OmegaT instance.

And also, the plug-in searches in comments that contains 
location informations about the string resources of the office.

[1] http://www.omegat.org/

[2] https://github.com/hanya/trhelper#help-viewer-hacker

## Usage

The plug-in is written in Groovy with OmegaT scripting plug-in[3], 
install it if you do not have it. Since OmegaT 3.0.3, the scripting 
plug-in has been integrated, so if you use 3.0.3 or later version, 
you do not need to install it yourself.

1. Make the OmegaT scripting work.
2. Start OmegaT.
3. Choose Tool - Scripting to open scripting window.
4. Push Choose button to select the directory that you put the plug-in file in.
5. Select the plug-in in the left side of the scripting window and push execute button.

Comment Search window is opened in step 6. You can search in comments 
of entries with the input field and the search button.
Push "Start" button to start your search server. 
Open your project before you use the plug-in.

The http server will be closed when you close the window.

[3] http://sourceforge.net/projects/omegat-plugins/files/OmegaT-Scripting/


## NOTICE

Released under GNU General Public License version 3.
