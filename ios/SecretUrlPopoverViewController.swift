/**
 * Copyright 2016 PhenixP2P Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import UIKit

protocol SecretUrlPopoverDelegate {
  func startComposingEmail()
}

final class SecretUrlPopoverViewController: UIViewController {
  @IBOutlet weak var scrollPopover: UIScrollView!
  @IBOutlet weak var serverList: UITextField!
  @IBOutlet weak var addressServer: UITextField!
  @IBOutlet weak var addressPCast: UITextField!
  @IBOutlet weak var buttonOk: UIButton!
  @IBOutlet weak var buttonCancel: UIButton!
  @IBOutlet weak var buttonEmail: UIButton!

  var serverPicker = UIPickerView()
  var arrayListServer = Array<ServerLocation>()
  var currentSelectedRow = Phenix.shared.serverOption
  var currentServerUri: String!
  var currentServerHttp: String!
  var currentServerName: String!
  var isManualSet = false
  var secretPopoverDelegate : SecretUrlPopoverDelegate?

  override func viewDidLoad() {
    super.viewDidLoad()
    self.setServerData()
    self.setSubviews()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    self.view.superview?.layer.cornerRadius = 5.0
  }

  override func didReceiveMemoryWarning() {
    super.didReceiveMemoryWarning()
  }

  func setSubviews() {
    self.enterManualSetting()
    self.scrollPopover.contentSize = CGSize.init(width: self.view.frame.size.width, height: self.view.frame.size.height * 0.4)
    self.self.scrollPopover.autoresizingMask = .flexibleHeight
    self.scrollPopover.isScrollEnabled = false
    self.scrollPopover.isScrollEnabled = true
    self.buttonOk.layer.cornerRadius = 5.0
    self.buttonOk.layer.masksToBounds = true
    self.buttonCancel.layer.cornerRadius = 5.0
    self.buttonCancel.layer.masksToBounds = true
    self.buttonEmail.layer.cornerRadius = 3.0
    self.buttonEmail.layer.masksToBounds = true
    let iconDown = UIImageView.init(frame: CGRect.init(x: 0, y: 0, width: 30, height: 30))
    iconDown.image = #imageLiteral(resourceName: "icon-menu-down")
    self.serverList.rightView = iconDown
    self.serverList.rightViewMode = .always

    // Server list picker
    self.serverPicker = UIPickerView(frame: CGRect.init(x: 0, y: 0, width: self.view.frame.width, height: 110))
    self.serverPicker.backgroundColor = PhenixColor.Gray
    self.serverPicker.showsSelectionIndicator = true
    self.serverPicker.delegate = self
    self.serverPicker.dataSource = self
    self.serverPicker.selectRow(Phenix.shared.serverOption, inComponent: 0, animated: false)
    self.pickerView(self.serverPicker, didSelectRow: Phenix.shared.serverOption, inComponent: 0)

    let toolBar = UIToolbar()
    toolBar.barStyle = .default
    toolBar.isTranslucent = true
    toolBar.tintColor = .white
    toolBar.sizeToFit()

    let doneButton = UIBarButtonItem(title: "Done", style: UIBarButtonItemStyle.plain, target: self, action: #selector(SecretUrlPopoverViewController.donePicker))
    doneButton.tintColor = PhenixColor.Gray
    let spaceButton = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.flexibleSpace, target: nil, action: nil)

    toolBar.setItems([spaceButton, doneButton], animated: false)
    toolBar.isUserInteractionEnabled = true

    self.serverList.inputView = self.serverPicker
    self.serverList.inputAccessoryView = toolBar
    self.currentServerHttp = self.addressServer.text
    self.currentServerUri = self.addressPCast.text
    self.currentServerName = self.serverList.text
    self.addressServer.delegate = self
    self.addressPCast.delegate = self
    self.addressServer.tag = 11
    self.addressPCast.tag = 12
  }

  func setServerData() {
    let dataArray = PhenixServerList
    for dataDict in dataArray {
      let info = ServerLocation.init(name: dataDict["name"]!, uri: dataDict["uri"]!, http: dataDict["http"]!)
      self.arrayListServer.append(info)
    }
  }

  func donePicker() {
    self.view.endEditing(true)
  }

  func enterManualSetting() {
    self.serverList.text = Phenix.shared.phenixInfo.name
    self.addressServer.text = Phenix.shared.phenixInfo.http
    self.addressPCast.text = Phenix.shared.phenixInfo.uri
  }

  @IBAction func buttonOkClicked(_ sender: Any) {
    self.view.endEditing(true)
    guard let textServer = self.addressServer.text, !textServer.isEmpty else {
      return
    }

    if Phenix.shared.phenixInfo.uri != self.addressPCast.text! || Phenix.shared.phenixInfo.http != textServer {
      Phenix.shared.phenixInfo.name = self.serverList.text!
      Phenix.shared.phenixInfo.uri = self.addressPCast.text!
      Phenix.shared.phenixInfo.http = textServer
      Phenix.shared.initPcast()
      Phenix.shared.serverOption = self.isManualSet ? 0 : self.currentSelectedRow
      NotificationCenter.default.post(name: Notification.Name(PhenixNotification.ServerChanged), object: nil)
    }
    self.dismiss(animated: true, completion: nil)
  }

  @IBAction func buttonCancelClicked(_ sender: Any) {
    self.view.endEditing(true)
    self.dismiss(animated: true, completion: nil)
  }

  @IBAction func buttonEmailLogClicked(_ sender: Any) {
    self.view.endEditing(true)
    self.dismiss(animated: true, completion: nil)
    if let delegate = secretPopoverDelegate {
      delegate.startComposingEmail()
    }
  }
}

extension SecretUrlPopoverViewController: UIPickerViewDataSource, UIPickerViewDelegate {

  func numberOfComponents(in pickerView: UIPickerView) -> Int {
    return 1
  }

  func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
    // Add 1 more row for 'Manual' option
    return self.arrayListServer.count + 1
  }

  func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
    if row == 0 {
      return "Manual"
    } else {
      let info = self.arrayListServer[row - 1]
      return info.name
    }
  }

  func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
    if row > 0 {
      let info = self.arrayListServer[row - 1]
      self.serverList.text = info.name
      self.addressServer.text = info.http
      self.addressPCast.text = info.uri
    } else {
      self.serverList.text = "Manual"
      self.addressServer.text = Phenix.shared.phenixInfo.http
      self.addressPCast.text = Phenix.shared.phenixInfo.uri
      self.donePicker()
      self.addressServer.becomeFirstResponder()
    }
    self.currentSelectedRow = row
  }

  func pickerView(_ pickerView: UIPickerView, viewForRow row: Int, forComponent component: Int, reusing view: UIView?) -> UIView {
    let sizeOfRow = pickerView.rowSize(forComponent: component)
    let customRowView = UIView.init(frame: CGRect(x: 0, y: 0, width: sizeOfRow.width, height: sizeOfRow.height))
    let pickerMarkImage = UIImageView.init(frame: CGRect(x: 10, y: (sizeOfRow.height / 2) - 10, width: 20.0, height: 20.0))
    if row == Phenix.shared.serverOption {
      pickerMarkImage.image = #imageLiteral(resourceName: "image-marked-circle")
      customRowView.addSubview(pickerMarkImage)
    }
    let pickerLabel = UILabel.init(frame: CGRect(x: 10, y: 0, width: sizeOfRow.width - 10, height: sizeOfRow.height))
    pickerLabel.textAlignment = .center
    if row == 0 {
      pickerLabel.text = "Manual"
    } else {
      let info = self.arrayListServer[row - 1]
      pickerLabel.text = info.name
    }
    customRowView.addSubview(pickerLabel)

    return customRowView
  }
}

extension SecretUrlPopoverViewController: UITextFieldDelegate {
  func textFieldShouldReturn(_ textField: UITextField) -> Bool {
    textField.resignFirstResponder()
    return true
  }

  func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
    let textFieldText: NSString = (textField.text ?? "") as NSString
    let updatedString = textFieldText.replacingCharacters(in: range, with: string)
    if textField.tag == self.addressServer.tag {
      if updatedString != self.currentServerHttp {
        self.updatePickerSetting(isManual: true)
      } else if updatedString == self.currentServerHttp, self.addressPCast.text == self.currentServerUri {
        self.updatePickerSetting(isManual: false)
      }
    } else if textField.tag == self.addressPCast.tag {
      if updatedString != self.currentServerUri {
        self.updatePickerSetting(isManual: true)
      } else if updatedString == self.currentServerUri, self.addressServer.text == self.currentServerHttp {
        self.updatePickerSetting(isManual: false)
      }
    }
    return true
  }

  private func updatePickerSetting(isManual: Bool) {
    if isManual {
      self.serverList.text = "Manual"
      Phenix.shared.serverOption = 0
    } else {
      self.serverList.text = self.currentServerName
      Phenix.shared.serverOption = self.currentSelectedRow
    }
    self.isManualSet = isManual
  }
}
