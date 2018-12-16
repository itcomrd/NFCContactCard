package com.deepoperation.nfccontactcard;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.view.Window;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class NfcContactMain extends Activity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
	private NfcAdapter m_NfcAdapter;
	private final static String[] PERMISSIONS = {Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS};
	private final static int REQUEST_PERMISSIONS = 2;
	private String m_Dispname;
	private String m_Phone1;
	private String m_Phone1_t;
	private String m_Email;
	private String m_Email_t;
	private String m_Conmpany;
	private String m_Website;
	private String m_Website_t;
	private String m_Address;
	private Switch m_SwPhone;
	private Switch m_SwMail;
	private Switch m_SwCompany;
	private Switch m_SwWebsite;
	private Switch m_SwAddress;
	private TextView	m_TextName;


	//アクティビティ起動時に呼ばれる
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

		//レイアウトの生成
		setContentView(R.layout.main);

		m_TextName = (TextView)this.findViewById(R.id.text3);
		m_SwPhone = (Switch)this.findViewById(R.id.sw1);
		m_SwMail = (Switch)this.findViewById(R.id.sw2);
		m_SwCompany = (Switch)this.findViewById(R.id.sw3);
		m_SwWebsite = (Switch)this.findViewById(R.id.sw4);
		m_SwAddress = (Switch)this.findViewById(R.id.sw5);


		//ユーザーの利用許可のチェック
		if(checkPermissions()) {
			makeInformation();
			//Androidビームの準備
			m_NfcAdapter = NfcAdapter.getDefaultAdapter(this);
			if (m_NfcAdapter != null) {
				m_NfcAdapter.setNdefPushMessageCallback(this, this);
				m_NfcAdapter.setOnNdefPushCompleteCallback(this, this);
			} else {
				toast(getString(R.string.nfc_hw_warning));
			}
		}
	}

	//インテント受信時に呼ばれる
	@Override
	public void onNewIntent(Intent intent) {
		setIntent(intent);
	}

	//アクティビティのレジューム時に呼ばれる
	@Override
	public void onResume() {
		super.onResume();
		//Androidビームの受信処理
		Intent intent = getIntent();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			Parcelable[] msgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage msg = (NdefMessage) msgs[0];
			addToContact(msg);
		}
	}

	private void addToContact(NdefMessage msg) {
		ArrayList<ContentProviderOperation> ops = new ArrayList<>();
		int rawContactInsertIndex = ops.size();

		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.RawContacts.CONTENT_URI)
				.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
				.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
				.build());

		// 氏名の設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, new String(msg.getRecords()[0].getPayload()))
				.build());

		// 電話番号の設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, new String(msg.getRecords()[1].getPayload()))
				.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, new String(msg.getRecords()[2].getPayload()))
				.build());

		// メールアドレスの設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Email.DATA, new String(msg.getRecords()[3].getPayload()))
				.withValue(ContactsContract.CommonDataKinds.Email.TYPE,  new String(msg.getRecords()[4].getPayload()))
				.build());

		// 会社名の設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, new String(msg.getRecords()[5].getPayload()))
				.build());

		// ウェブサイトの設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Website.URL, new String(msg.getRecords()[6].getPayload()))
				.withValue(ContactsContract.CommonDataKinds.Website.TYPE, new String(msg.getRecords()[7].getPayload()))
				.build());

		// 住所の設定
		ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
				.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, new String(msg.getRecords()[8].getPayload()))
				.build());
		try {
			ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

		} catch (Exception e) {
//			Log.e("ContactListActivity", e.getLocalizedMessage(), e);
			e.printStackTrace();
		}
		finish();
	}


	//Androidビームが可能な別端末が範囲内にある時に呼ばれる
	@Override
	public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
		String phone1 = m_Phone1;
		String phone1_t = m_Phone1_t;
		String email = m_Email;
		String email_t = m_Email_t;
		String company = m_Conmpany;
		String website = m_Website;
		String website_t = m_Website_t;
		String address = m_Address;
		if (m_SwPhone.isChecked() == false){
			phone1 = "";
			phone1_t = "";
		}
		if (m_SwMail.isChecked() == false){
			email = "";
			email_t = "";
		}
		if (m_SwCompany.isChecked() == false){
			company = "";
		}
		if (m_SwWebsite.isChecked() == false){
			website = "";
			website_t = "";
		}
		if (m_SwAddress.isChecked() == false){
			address = "";
		}

		//Androidビームのメッセージの生成
		NdefMessage msg = new NdefMessage(
				new NdefRecord[]{
						//MIMEタイプ含むレコードの生成
						createMimeRecord("application/com.deepoperation.nfccontactcard", m_Dispname.getBytes()),

						createMimeRecord("application/com.deepoperation.nfccontactcard", phone1.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", phone1_t.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", email.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", email_t.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", company.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", website.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", website_t.getBytes()),
						createMimeRecord("application/com.deepoperation.nfccontactcard", address.getBytes()),
						//AAR含むレコードの生成
						NdefRecord.createApplicationRecord("com.deepoperation.nfccontactcard")
				});
		return msg;
	}

	private NdefRecord createMimeRecord(String mimeType, byte[] payload) {
		byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
		NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
		return mimeRecord;
	}

	//Androidビームの送信完了時に呼ばれる
	@Override
	public void onNdefPushComplete(NfcEvent event) {
		Handler handler = new Handler();
		handler.post(new Runnable() {
			@Override
			public void run() {
				toast(getString(R.string.nfc_sent));
			}
		});
	}


	//ユーザーの利用許可のチェック
	private boolean checkPermissions() {
		//未許可
		boolean ret = isGranted();
		if (!ret) {
			//許可ダイアログの表示
			ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
		}
		return ret;
	}

	//ユーザーの利用許可が済かどうかの取得
	private boolean isGranted() {
		for (int i = 0; i < PERMISSIONS.length; i++) {
			if (PermissionChecker.checkSelfPermission(NfcContactMain.this, PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	//許可ダイアログ選択時に呼ばれる
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] results) {
		if (requestCode == REQUEST_PERMISSIONS) {
			//未許可
			if (!isGranted()) {
				toast(getString(R.string.no_permission));
			} else {
				makeInformation();
				//Androidビームの準備
				m_NfcAdapter = NfcAdapter.getDefaultAdapter(this);
				if (m_NfcAdapter != null) {
					m_NfcAdapter.setNdefPushMessageCallback(this, this);
					m_NfcAdapter.setOnNdefPushCompleteCallback(this, this);
				} else {
					toast(getString(R.string.nfc_hw_warning));
				}
			}
		} else {
			super.onRequestPermissionsResult(requestCode, permissions, results);
		}
	}

	//トーストの表示
	private void toast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	private void makeInformation() {

		m_Dispname = queryInfo (	new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME});
		m_Phone1 = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER});
		m_Phone1_t = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
				new String[]{ContactsContract.CommonDataKinds.Phone.TYPE});
		m_Email = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS});
		m_Email_t = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE},
				new String[]{ContactsContract.CommonDataKinds.Email.TYPE});
		m_Conmpany = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.Organization.COMPANY});
		m_Website = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.Website.URL});
		m_Website_t = queryInfo (	new String[]{ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE},
				new String[]{ContactsContract.CommonDataKinds.Website.TYPE});
		m_Address = queryInfo (	new String[]{ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE},
							new String[]{ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS});

		m_TextName.append(m_Dispname);
		m_SwPhone.append(m_Phone1);
		m_SwMail.append(m_Email);
		m_SwCompany.append(m_Conmpany);
		m_SwWebsite.append(m_Website);
		m_SwAddress.append(m_Address);

	}

	private String queryInfo (String [] wherep2, String [] projection) {
		ContentResolver content = getContentResolver();
		Cursor cursor = content.query(
				Uri.withAppendedPath(
						ContactsContract.Profile.CONTENT_URI,
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY),
						projection,
					ContactsContract.Contacts.Data.MIMETYPE + "=?",
						wherep2,
					null
			);

		String number ="";
		if (cursor != null) {
			String[] columns = cursor.getColumnNames();
			while (cursor.moveToNext()) {
				for (String column : columns) {
					number = cursor.getString(cursor.getColumnIndex(column));
				}
			}
			cursor.close();
		}
		return number;
	}
}
