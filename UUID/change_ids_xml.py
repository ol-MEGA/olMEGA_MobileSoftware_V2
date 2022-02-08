
import tkinter as tk
from tkinter import filedialog
import uuid
import os
import xml.etree.ElementTree as ET

uuids = {}


def main():
    """Function that replaces all ids and corresponding filters in xml questionnaire by UUIDs

    UUIDs are Universally Unique Identifiers, which exist only in one instance and therefore represent a secure means to
    individually classify a given entity. They are used as keys to the olMEGA database, so no two questions may be saved
    under the same key.
    Your input file must contain a valid .xml structure, otherwise the script will raise an exception and report an
    error.
    """

    root = tk.Tk()
    root.withdraw()
    extension = ".xml"
    file_path = filedialog.askopenfilename(title="Please select questionnaire *.xml file", filetypes=(("Quest files", extension),))

    open(f"{os.path.splitext(file_path)[0]}_UUID{extension}", 'w').close()
    file_path_new = f"{os.path.splitext(file_path)[0]}_UUID{extension}"

    print(f"File selected: {file_path}")

    try:
        tree = ET.parse(file_path)
        root = tree.getroot()

        # find and replace option ids - necessary to do first because option ids might include question ids in string
        for item in root.iter('option'):
            item.attrib['id'] = put_and_get_id(item.attrib['id'])

        # find and replace ids in question head
        for child in root:

            # Find all filters and store them in uuid dict
            if 'filter' in child.attrib:
                filter_list = child.attrib['filter'].split(',')
                for filter in filter_list:
                    filter_stripped = filter.strip()
                    if filter_stripped.startswith('!'):
                        put_and_get_id(filter_stripped[1:])
                    else:
                        put_and_get_id(filter_stripped)

                # replace filter ids in questionnaire
                filter_string = child.attrib['filter']
                for id in uuids:
                    filter_string = filter_string.replace(id, uuids[id])
                child.attrib['filter'] = filter_string

            # replace all option ids
            if 'option' in child.attrib:
                print(child.attrib['option'])


        # replace all question ids
        for child in root:
            if 'id' in child.attrib:
                child.attrib['id'] = put_and_get_id(child.attrib['id'])

        tree.write(file_path_new, encoding='UTF-8', xml_declaration=True)

        print(f'Success - {len(uuids)} ids were exchanged.')

    except ET.ParseError as e:
        print(f'Not a valid xml file.\nError: {e}')


def put_and_get_id(id):
    """Function to check whether id is already in storage and if not - create new key and uuid value

    """

    if id not in uuids:
        # store id with UUID in dictionary
        uuids[id] = str(uuid.uuid1())
    return uuids[id]


if __name__ == '__main__':
    main()