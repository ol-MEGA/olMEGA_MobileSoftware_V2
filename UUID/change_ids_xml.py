
import tkinter as tk
from tkinter import filedialog
import uuid
import os
import xml.etree.ElementTree as ET

uuids = {}

def main():
    root = tk.Tk()
    root.withdraw()
    extension = ".xml"
    # file_path = filedialog.askopenfilename(title="Please select questionnaire *.xml file", filetypes=(("Quest files", extension),))
    file_path = "E:\olMEGA_MobileSoftware_V2\quest\HAPPAA_01-1_21-04-15.xml"

    open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", 'w').close()
    file_path_new = f"{os.path.splitext(file_path)[0]}_UUID{extension}"

    print(f"File selected: {file_path}")

    f_original = open(file_path, "r")
    lines = f_original.readlines()
    f_original.close()




    tree = ET.parse(file_path)
    root = tree.getroot()

    for child in root:
        if 'id' in child.attrib:
            child.attrib['id'] = exchange_id(child.attrib['id'])

    for child in root:
        if 'id' in child.attrib:
            print(child.attrib['id'])

        # if 'filter' in child.attrib:
        #     print(child.attrib['filter'])





    # f_new.write(new_line)

    tree.write(file_path_new, encoding='UTF-8', xml_declaration=True)


    # f_new.close()

print(f'Success - {len(uuids)} ids were exchanged.')


def exchange_id(id):
    if id not in uuids:
        # store id with UUID in dictionary
        uuids[id] = str(uuid.uuid1())
    return uuids[id]


if __name__ == '__main__':
    main()