
import tkinter as tk
from tkinter import filedialog
import uuid
import os

root = tk.Tk()
root.withdraw()
extension = ".xml"
file_path = filedialog.askopenfilename(title="Please select questionnaire *.xml file", filetypes=(("Quest files", extension),))

open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", 'w').close()
f_new = open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", "a")

print(f"File selected: {file_path}")

identifiers = ['id="', 'filter="']

f_original = open(file_path, "r")
lines = f_original.readlines()

uuids = {}

for line in lines:

    parts = line.split(" ")

    for idx, part in enumerate(parts):

        # question or answer ids
        if part.__contains__('id="'):

            ids = part.split('id="')
            id_parts = ids[1].split('"')
            id = id_parts[0]

            if id not in uuids:
                # store id with UUID in dictionary
                uuids[id] = str(uuid.uuid1())

            id_parts[0] = f'{uuids[id]}"'
            ids[1] = f'id="{"".join(id_parts)}'
            part = "".join(ids)
            parts[idx] = "".join(part)

        # filter ids
        if part.__contains__('filter="'):

            id_line = part.split('filter="')
            id_parts = id_line[1].split('"')
            id_cluster = id_parts[0]
            id_list = id_cluster.split(",")

            # multiple filters possible
            for idx_filter, id in enumerate(id_list):
                if '!' in id:
                    # negative criterion
                    id_list[idx_filter] = f'!{uuids[id[1:]]}'
                else:
                    # positive criterion
                    id_list[idx_filter] = uuids[id]

            id_cluster = ",".join(id_list)
            id_parts[0] = id_cluster + '"'
            id_line[1] = f'filter="{"".join(id_parts)}'

            part = "".join(id_line)
            parts[idx] = part

    new_line = " ".join(parts)
    f_new.write(new_line)

f_original.close()
f_new.close()

print(f'Success - {len(uuids)} ids were exchanged.')