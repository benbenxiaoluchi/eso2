//
//  SSOExtensionHelpViewController.h
//  Accenture SSO
//
//  Created by Joshua Lamkin on 9/29/15.
//  Copyright © 2015 Accenture. All rights reserved.
//

#import <UIKit/UIKit.h>


@protocol SSOExtensionHelpViewControllerDelegate <NSObject>
@required
- (void)neverShowAgain;
- (void)showSignIn;

@end

@interface SSOExtensionHelpViewController : UIViewController

@property (nonatomic, weak) id <SSOExtensionHelpViewControllerDelegate> delegate;

- (IBAction)skipButtonTapped:(id)sender;

@property (nonatomic, weak) IBOutlet UIButton *skipButton;
@property (nonatomic, weak) IBOutlet UIPageControl *pageControl;

@end
